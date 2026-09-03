package com.omni.recommender.behavior.infrastructure.adapter;

import com.omni.recommender.behavior.application.port.out.BehaviorHBasePort;
import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import com.omni.recommender.behavior.domain.behavior.aggregate.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BehaviorHBaseAdapter implements BehaviorHBasePort {

    private final Connection hbaseConnection;
    private static final String TABLE_NAME = "user_behavior_history";
    private static final byte[] CF = Bytes.toBytes("cf");

    @Override
    public void writeBehavior(UserBehavior behavior) {
        if (behavior.getUserId() == null) return;
        
        try (Table table = hbaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            // RowKey: userId + "_" + (Long.MAX_VALUE - timestamp)
            String userId = behavior.getUserId().value();
            long reverseTimestamp = Long.MAX_VALUE - behavior.getTimestamp().toEpochMilli();
            String rowKey = userId + "_" + reverseTimestamp;

            Put put = new Put(Bytes.toBytes(rowKey));
            
            if (behavior.getItemId() != null) {
                put.addColumn(CF, Bytes.toBytes("itemId"), Bytes.toBytes(behavior.getItemId().value()));
            }
            if (behavior.getBehaviorType() != null) {
                put.addColumn(CF, Bytes.toBytes("behaviorType"), Bytes.toBytes(behavior.getBehaviorType().name()));
            }
            put.addColumn(CF, Bytes.toBytes("timestamp"), Bytes.toBytes(String.valueOf(behavior.getTimestamp().toEpochMilli())));

            table.put(put);
            log.info("Successfully wrote behavior to HBase with RowKey: {}", rowKey);
        } catch (IOException e) {
            log.error("Failed to write behavior to HBase", e);
        }
    }

    @Override
    public List<UserBehavior> getRecentBehaviors(String userId, int limit) {
        List<UserBehavior> behaviors = new ArrayList<>();
        
        try (Table table = hbaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            Scan scan = new Scan();
            // Start scanning from userId_
            scan.withStartRow(Bytes.toBytes(userId + "_"));
            // Stop scanning at userId_~ (which is lexically after any numbers)
            scan.withStopRow(Bytes.toBytes(userId + "_{")); // '{' is char after 'z', but we use digits so it's fine. Wait, better to use userId + "`"
            scan.setLimit(limit);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    byte[] itemIdBytes = result.getValue(CF, Bytes.toBytes("itemId"));
                    byte[] typeBytes = result.getValue(CF, Bytes.toBytes("behaviorType"));
                    byte[] tsBytes = result.getValue(CF, Bytes.toBytes("timestamp"));

                    String itemId = itemIdBytes != null ? Bytes.toString(itemIdBytes) : null;
                    String behaviorType = typeBytes != null ? Bytes.toString(typeBytes) : "VIEW";
                    long timestamp = tsBytes != null ? Long.parseLong(Bytes.toString(tsBytes)) : System.currentTimeMillis();

                    UserBehavior behavior = UserBehavior.log(
                        new UserId(userId),
                        null,
                        itemId != null ? new ItemId(itemId) : null,
                        BehaviorType.valueOf(behaviorType.toUpperCase()),
                        new DeviceInfo("unknown", "unknown"),
                        null,
                        new BehaviorContext(null),
                        java.time.Instant.ofEpochMilli(timestamp)
                    );
                    behaviors.add(behavior);
                }
            }
        } catch (Exception e) {
            log.error("Failed to read behaviors from HBase for user: {}", userId, e);
        }
        
        return behaviors;
    }
}
