package com.omni.recommender.recommendation.application.service;

import com.omni.recommender.recommendation.application.port.in.GetRecommendationUseCase;
import com.omni.recommender.recommendation.application.port.out.RecommendationHBasePort;
import com.omni.recommender.recommendation.application.query.GetRecommendationQuery;
import com.omni.recommender.recommendation.application.view.RecommendationGottenView;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.root.Recommendation;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.RecommendedItem;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 負責查詢推薦清單的 Application Service (Query Service)
 * 依據 AGENTS.md 規範：宣告為 package-private，不加上 public。
 */
@Service
class RecommendationQueryService implements GetRecommendationUseCase {

    private final RecommendationHBasePort hbasePort;

    // 定義降級策略使用的預設熱門行程清單
    private static final List<RecommendedItem> DEFAULT_POPULAR_ITEMS = List.of(
            new RecommendedItem("TPE-NRT-PROMO", 0.99, 1),
            new RecommendedItem("KHH-KIX-HOT", 0.95, 2),
            new RecommendedItem("TPE-BKK-SALE", 0.88, 3)
    );

    public RecommendationQueryService(RecommendationHBasePort hbasePort) {
        this.hbasePort = hbasePort;
    }

    @Override
    public RecommendationGottenView execute(GetRecommendationQuery query) {
        UserId userId = new UserId(query.userId());

        // 1. 呼叫 Outbound Port 查詢資料
        Recommendation recommendation = hbasePort.fetchRecommendation(userId)
                // 2. 若發生 Cache Miss，則建立降級 (Fallback) 的預設領域模型
                .orElseGet(() -> Recommendation.fallback(userId, DEFAULT_POPULAR_ITEMS));

        // 3. 領域模型轉 View Projection，限制回傳筆數 (limit)
        List<RecommendationGottenView.ItemView> itemViews = recommendation.getItems().stream()
                .limit(query.limit())
                .map(item -> new RecommendationGottenView.ItemView(item.itemId(), item.score(), item.rank()))
                .collect(Collectors.toList());

        return new RecommendationGottenView(
                recommendation.getUserId().value(),
                itemViews,
                recommendation.getGeneratedAt(),
                recommendation.isFallback()
        );
    }
}
