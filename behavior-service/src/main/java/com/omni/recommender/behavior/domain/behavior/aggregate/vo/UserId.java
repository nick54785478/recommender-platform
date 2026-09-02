package com.omni.recommender.behavior.domain.behavior.aggregate.vo;

/**
 * 用戶 ID (Value Object)
 * 使用 Java Record 確保不可變性 (Immutability) 並提供內建的 equals/hashCode
 */
public record UserId(String value) {
    
    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }
}
