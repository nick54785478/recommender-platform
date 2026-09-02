package com.omni.recommender.behavior.domain.behavior.aggregate.vo;

/**
 * 裝置與連線資訊 (Value Object)
 * 使用 Java Record 確保不可變性 (Immutability)
 */
public record DeviceInfo(String clientIp, String userAgent) {
}
