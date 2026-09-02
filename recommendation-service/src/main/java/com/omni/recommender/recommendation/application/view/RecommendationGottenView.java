package com.omni.recommender.recommendation.application.view;

import java.time.Instant;
import java.util.List;

/**
 * 推薦清單查詢結果視圖 (View Projection)
 * 遵循 N + Gotten + View 命名規範，嚴格作為純資料載體。
 */
public record RecommendationGottenView(
        String userId,
        List<ItemView> items,
        Instant generatedAt,
        boolean isFallback
) {
    public record ItemView(String itemId, double score, int rank) {}
}
