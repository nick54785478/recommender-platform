package com.omni.recommender.behavior.domain.behavior.aggregate.vo;

/**
 * 商品/行程 ID (Value Object)
 * 使用 Java Record 確保不可變性 (Immutability)
 */
public record ItemId(String value) {

    public ItemId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be empty");
        }
    }
}
