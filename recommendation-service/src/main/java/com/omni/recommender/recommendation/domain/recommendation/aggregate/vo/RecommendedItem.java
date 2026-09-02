package com.omni.recommender.recommendation.domain.recommendation.aggregate.vo;

/**
 * 單筆推薦行程/商品 (Value Object)
 */
public record RecommendedItem(
        String itemId,
        double score,
        int rank
) {
    public RecommendedItem {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("ItemId cannot be empty");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("Rank must be at least 1");
        }
    }
}
