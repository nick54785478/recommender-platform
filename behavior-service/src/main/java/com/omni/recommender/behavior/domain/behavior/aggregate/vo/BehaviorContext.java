package com.omni.recommender.behavior.domain.behavior.aggregate.vo;

import java.util.Collections;
import java.util.Map;

/**
 * 彈性行為上下文資料 (Value Object)
 * 使用 Java Record 確保不可變性 (Immutability)
 */
public record BehaviorContext(Map<String, String> data) {
    
    // 緊湊建構子 (Compact Constructor)，確保傳入的 Map 是不可變的
    public BehaviorContext {
        data = data != null ? Map.copyOf(data) : Collections.emptyMap();
    }

    /**
     * 檢查是否包含特定鍵值
     */
    public boolean hasKey(String key) {
        return this.data.containsKey(key);
    }

    /**
     * 取得特定鍵值
     */
    public String getValue(String key) {
        return this.data.get(key);
    }
}
