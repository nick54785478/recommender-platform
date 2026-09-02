package com.omni.recommender.recommendation.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omni.recommender.recommendation.application.port.out.RecommendationHBasePort;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.root.Recommendation;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.RecommendedItem;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HBase 資料庫存取適配器 (Outbound Adapter)
 * 依照規範宣告為 package-private。
 * 已實作真實的 HBase 連線查詢，解析 Spark 寫入的 JSON 推薦清單。
 */
@Slf4j
@Component
class RecommendationHBaseAdapter implements RecommendationHBasePort {

    private final Configuration hbaseConfig;
    private final ObjectMapper objectMapper;
    private static final byte[] CF = Bytes.toBytes("cf");
    private static final byte[] QUALIFIER = Bytes.toBytes("items");

    public RecommendationHBaseAdapter(Configuration hbaseConfig, ObjectMapper objectMapper) {
        this.hbaseConfig = hbaseConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Recommendation> fetchRecommendation(UserId userId) {
        log.info("Fetching recommendations for user {} from HBase...", userId.value());
        
        try (Connection connection = ConnectionFactory.createConnection(hbaseConfig);
             Table table = connection.getTable(TableName.valueOf("recommendation"))) {
            
            Get get = new Get(Bytes.toBytes(userId.value()));
            Result result = table.get(get);
            
            if (result == null || result.isEmpty()) {
                log.info("Cache miss in HBase for user {}", userId.value());
                return Optional.empty(); // 觸發 Domain Fallback 降級
            }
            
            byte[] valueBytes = result.getValue(CF, QUALIFIER);
            if (valueBytes == null || valueBytes.length == 0) {
                return Optional.empty();
            }
            
            // 解析 Spark 寫入的 JSON 格式: [{"itemId":"...", "score":0.99}, ...]
            String jsonItems = Bytes.toString(valueBytes);
            JsonNode rootArray = objectMapper.readTree(jsonItems);
            
            List<RecommendedItem> items = new ArrayList<>();
            int rank = 1;
            
            // Spark ALS 產出的陣列預設已經是依照分數排序的 Top N
            for (JsonNode node : rootArray) {
                String itemId = node.get("itemId").asText();
                double score = node.get("score").asDouble();
                // 補充領域層必須的 rank 屬性
                items.add(new RecommendedItem(itemId, score, rank++));
            }
            
            log.info("Successfully fetched {} recommendations from HBase for user {}", items.size(), userId.value());
            return Optional.of(Recommendation.restore(userId, items, Instant.now()));
            
        } catch (Exception e) {
            log.error("Failed to fetch recommendation from HBase for user " + userId.value() + ". Triggering fallback.", e);
            // 發生異常 (如 HBase 當機) 時，為了系統穩定性，安全回傳空值以觸發熱門行程 Fallback
            return Optional.empty();
        }
    }
}
