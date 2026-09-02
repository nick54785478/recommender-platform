package com.omni.recommender.spark.sink;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 負責將推薦結果批次寫入 HBase 的 Sink 端
 */
@Slf4j
public class HBaseWriter {

    /**
     * 將 Spark 產出的結果寫入 HBase
     * @param jsonRecommendations DataFrame 需要包含 `userId` (String) 與 `itemsJson` (String) 兩個欄位
     * @param zookeeperQuorum HBase Zookeeper 的連線位址
     */
    public void write(Dataset<Row> jsonRecommendations, String zookeeperQuorum) {
        log.info("Writing recommendation results to HBase (Quorum: {})...", zookeeperQuorum);
        
        // 透過 foreachPartition 在各個 Executor 的分區內獨立建立 HBase 連線，提升寫入效能
        jsonRecommendations.foreachPartition(iterator -> {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", zookeeperQuorum);
            conf.set("hbase.zookeeper.property.clientPort", "2181");
            
            try (Connection connection = ConnectionFactory.createConnection(conf);
                 Table table = connection.getTable(TableName.valueOf("recommendation"))) {
                 
                List<Put> puts = new ArrayList<>();
                byte[] cf = Bytes.toBytes("cf");
                byte[] qualifier = Bytes.toBytes("items");
                
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    String userId = row.getAs("userId");
                    String itemsJson = row.getAs("itemsJson");
                    
                    // 以 userId 作為 RowKey，能保證後端 API 以 O(1) 極速查詢
                    Put put = new Put(Bytes.toBytes(userId));
                    put.addColumn(cf, qualifier, Bytes.toBytes(itemsJson));
                    puts.add(put);
                    
                    // 實作批次寫入 (Batch Put)，每 1000 筆 flush 一次以減輕 HBase 負擔
                    if (puts.size() >= 1000) {
                        table.put(puts);
                        puts.clear();
                    }
                }
                // 寫入剩餘的資料
                if (!puts.isEmpty()) {
                    table.put(puts);
                }
            } catch (IOException e) {
                log.error("Failed to write partition data to HBase", e);
                throw new RuntimeException("HBase writing failed", e);
            }
        });
        
        log.info("Successfully wrote all recommendations to HBase.");
    }
}
