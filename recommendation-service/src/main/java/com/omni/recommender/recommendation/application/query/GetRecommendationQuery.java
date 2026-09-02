package com.omni.recommender.recommendation.application.query;

/**
 * 獲取推薦清單的查詢物件 (Query)
 * 遵循 V + N + Query 命名規範，且做為純資料載體 (Record)
 */
public record GetRecommendationQuery(
        String userId,
        int limit
) {
    public GetRecommendationQuery {
        if (limit <= 0) {
            limit = 10; // 預設最多 10 筆
        }
    }
}
