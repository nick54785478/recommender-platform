package com.omni.recommender.recommendation.presentation.resource.out;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 推薦清單 API 回傳資源 (Response Resource)
 * 遵循 N + Ved + Resource 命名規範。
 * 作為純資料載體 (Pure Data Carrier)。
 */
@Data
public class RecommendationGottenResource {
    private String userId;
    private List<ItemResource> items;
    private Instant generatedAt;
    private boolean fallback;

    @Data
    public static class ItemResource {
        private String itemId;
        private double score;
        private int rank;
    }
}
