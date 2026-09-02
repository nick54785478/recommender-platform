package com.omni.recommender.recommendation.application.port.in;

import com.omni.recommender.recommendation.application.query.GetRecommendationQuery;
import com.omni.recommender.recommendation.application.view.RecommendationGottenView;

/**
 * 獲取推薦清單的 Inbound Port (UseCase)
 * 遵循 ... + UseCase 命名規範。
 */
public interface GetRecommendationUseCase {
    /**
     * 執行查詢
     * @param query 查詢條件
     * @return 查詢結果視圖 (View)
     */
    RecommendationGottenView execute(GetRecommendationQuery query);
}
