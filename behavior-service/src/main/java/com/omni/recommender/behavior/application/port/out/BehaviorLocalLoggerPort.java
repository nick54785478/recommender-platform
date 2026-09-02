package com.omni.recommender.behavior.application.port.out;

import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;

/**
 * 【Outbound Port (Driven Port)】
 * 負責定義將「用戶行為日誌」寫入本地儲存媒介的抽象介面。
 * 
 * 依照 Clean Architecture 規範，應用層 (Application Layer) 僅依賴此介面，
 * 實際的寫入細節 (如寫入磁碟、Kafka 或其他本地快取機制) 將交由基礎設施層 (Infrastructure Layer) 的 Adapter 實作。
 */
public interface BehaviorLocalLoggerPort {
    
    /**
     * 將指定的用戶行為紀錄 (UserBehavior) 寫入本地日誌。
     * 
     * 實作注意事項：
     * 此操作為前端直接呼叫路徑上的其中一環，應盡可能確保極低延遲 (Ultra-low latency)，
     * 建議使用非同步機制 (如 AsyncAppender 或 Message Queue) 避免阻塞主執行緒。
     *
     * @param userBehavior 已驗證且封裝完成的用戶行為聚合根實體
     */
    void writeLog(UserBehavior userBehavior);
}
