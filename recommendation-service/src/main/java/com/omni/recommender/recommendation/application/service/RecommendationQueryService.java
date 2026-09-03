package com.omni.recommender.recommendation.application.service;

import com.omni.recommender.recommendation.application.port.in.GetRecommendationUseCase;
import com.omni.recommender.recommendation.application.port.out.RecommendationHBasePort;
import com.omni.recommender.recommendation.application.query.GetRecommendationQuery;
import com.omni.recommender.recommendation.application.view.RecommendationGottenView;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.root.Recommendation;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.RecommendedItem;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;
import com.omni.recommender.recommendation.infrastructure.catalog.ItemCatalog;
import com.omni.recommender.recommendation.infrastructure.client.BehaviorClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 負責查詢推薦清單的 Application Service (Query Service)
 */
@Service
class RecommendationQueryService implements GetRecommendationUseCase {

    private final RecommendationHBasePort hbasePort;
    private final BehaviorClient behaviorClient;
    private final ItemCatalog itemCatalog;

    // 定義降級策略使用的預設熱門行程清單
    private static final List<RecommendedItem> DEFAULT_POPULAR_ITEMS = List.of(
            new RecommendedItem("TPE-NRT-PROMO", 0.99, 1),
            new RecommendedItem("KHH-KIX-HOT", 0.95, 2),
            new RecommendedItem("TPE-BKK-SALE", 0.88, 3)
    );

    public RecommendationQueryService(RecommendationHBasePort hbasePort, BehaviorClient behaviorClient, ItemCatalog itemCatalog) {
        this.hbasePort = hbasePort;
        this.behaviorClient = behaviorClient;
        this.itemCatalog = itemCatalog;
    }

    @Override
    public RecommendationGottenView execute(GetRecommendationQuery query) {
        UserId userId = new UserId(query.userId());

        // POC 階段：直接啟用即時「行為與標籤 (Content-based)」打分引擎
        List<RecommendedItem> realTimeItems = computeRealTimeRecommendations(query.userId());
        
        Recommendation finalRecommendation;
        if (!realTimeItems.isEmpty()) {
            finalRecommendation = Recommendation.restore(userId, realTimeItems, Instant.now());
        } else {
            // 如果沒有即時行為歷史，嘗試從 HBase 獲取 Spark 離線運算好的推薦清單
            Optional<Recommendation> offlineRec = hbasePort.fetchRecommendation(userId);
            if (offlineRec.isPresent()) {
                finalRecommendation = offlineRec.get();
            } else {
                // 真的沒有歷史資料，則使用 Fallback
                finalRecommendation = Recommendation.fallback(userId, DEFAULT_POPULAR_ITEMS);
            }
        }

        List<RecommendationGottenView.ItemView> itemViews = finalRecommendation.getItems().stream()
                .limit(query.limit())
                .map(item -> new RecommendationGottenView.ItemView(item.itemId(), item.score(), item.rank()))
                .collect(Collectors.toList());

        return new RecommendationGottenView(
                finalRecommendation.getUserId().value(),
                itemViews,
                finalRecommendation.getGeneratedAt(),
                finalRecommendation.isFallback()
        );
    }

    private List<RecommendedItem> computeRealTimeRecommendations(String userId) {
        List<Map<String, Object>> behaviors = behaviorClient.getRecentBehaviors(userId);
        if (behaviors == null || behaviors.isEmpty()) {
            return Collections.emptyList();
        }

        // 計算使用者的標籤偏好權重 (User Tag Profile)
        Map<String, Double> userTagProfile = new HashMap<>();
        Set<String> interactedItemIds = new HashSet<>();

        for (Map<String, Object> b : behaviors) {
            String itemId = (String) b.get("itemId");
            String type = (String) b.get("behaviorType");
            
            if (itemId == null || itemId.isEmpty()) continue; // SEARCH behavior might have empty itemId
            interactedItemIds.add(itemId);
            
            double weight = switch (type.toUpperCase()) {
                case "PURCHASE" -> 5.0;
                case "ADD_TO_CART" -> 4.0;
                case "LIKE" -> 3.0;
                case "VIEW" -> 1.0;
                default -> 0.5;
            };

            ItemCatalog.ItineraryItem item = itemCatalog.getItem(itemId);
            if (item != null) {
                for (String tag : item.getTags()) {
                    userTagProfile.put(tag, userTagProfile.getOrDefault(tag, 0.0) + weight);
                }
            }
        }

        if (userTagProfile.isEmpty()) return Collections.emptyList();

        // 對所有商品進行打分 (Cosine Similarity / Weighted Sum)
        List<RecommendedItem> scoredItems = new ArrayList<>();
        for (ItemCatalog.ItineraryItem candidate : itemCatalog.getAllItems()) {
            double score = 0.0;
            for (String tag : candidate.getTags()) {
                score += userTagProfile.getOrDefault(tag, 0.0);
            }
            
            // 已經互動過的商品降權 (Penalty)，鼓勵探索新商品
            if (interactedItemIds.contains(candidate.getId())) {
                score = score * 0.1;
            }

            if (score > 0) {
                // 正規化到 0~0.99 之間做展示
                double normalizedScore = Math.min(0.99, score / 20.0); 
                scoredItems.add(new RecommendedItem(candidate.getId(), normalizedScore, 1));
            }
        }

        // 依分數排序並賦予 Rank
        scoredItems.sort((a, b) -> Double.compare(b.score(), a.score()));
        
        List<RecommendedItem> result = new ArrayList<>();
        int rank = 1;
        for (RecommendedItem item : scoredItems) {
            result.add(new RecommendedItem(item.itemId(), item.score(), rank++));
        }

        return result;
    }
}
