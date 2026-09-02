package com.omni.recommender.recommendation.domain.recommendation.aggregate.vo;

/**
 * 用戶 ID (Value Object)
 */
public record UserId(String value) {
    public UserId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId cannot be empty");
        }
    }
}
