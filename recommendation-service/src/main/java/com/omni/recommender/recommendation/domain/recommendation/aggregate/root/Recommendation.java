package com.omni.recommender.recommendation.domain.recommendation.aggregate.root;

import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.RecommendedItem;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 推薦清單 (Aggregate Root)
 * 封裝專屬於單一用戶的推薦項目清單與相關詮釋資料。
 * 嚴格遵守 Pure Domain 規範，無外部框架侵入。
 */
public class Recommendation {

    private final UserId userId;
    private final List<RecommendedItem> items;
    private final Instant generatedAt;
    private final boolean isFallback;

    private Recommendation(UserId userId, List<RecommendedItem> items, Instant generatedAt, boolean isFallback) {
        this.userId = userId;
        // 確保內部清單不可變 (Immutability)
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
        this.generatedAt = generatedAt != null ? generatedAt : Instant.now();
        this.isFallback = isFallback;
    }

    /**
     * 從資料庫/HBase 重建模型
     */
    public static Recommendation restore(UserId userId, List<RecommendedItem> items, Instant generatedAt) {
        return new Recommendation(userId, items, generatedAt, false);
    }

    /**
     * 建立預設/熱門行程的降級模型 (Cache Miss Fallback)
     */
    public static Recommendation fallback(UserId userId, List<RecommendedItem> defaultItems) {
        return new Recommendation(userId, defaultItems, Instant.now(), true);
    }

    // --- Getters (無 Lombok) ---
    public UserId getUserId() {
        return userId;
    }

    public List<RecommendedItem> getItems() {
        return items;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public boolean isFallback() {
        return isFallback;
    }
}
