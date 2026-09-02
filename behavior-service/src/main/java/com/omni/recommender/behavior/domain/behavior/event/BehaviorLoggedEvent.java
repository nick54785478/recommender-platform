package com.omni.recommender.behavior.domain.behavior.event;

import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;

import java.time.Instant;
import java.util.Map;

/**
 * 領域事件：行為已記錄
 * 當 UserBehavior 建立完成後所觸發的事件。
 * 採用 Java Record 以確保事件資料不可變 (Immutable)，且不依賴外部框架。
 *
 * @param behaviorId   系統內部唯一識別碼
 * @param userId       關聯的用戶 ID (未登入則可能為 null)
 * @param sessionId    關聯的工作階段 ID
 * @param itemId       該行為所互動的目標商品/行程 ID
 * @param behaviorType 具體的行為類型字串 (如 VIEW, CLICK)
 * @param clientIp     使用者來源 IP 位址
 * @param userAgent    使用者的瀏覽器或裝置資訊
 * @param referrerUrl  來源網址
 * @param metadata     特定行為的擴充資料
 * @param timestamp    行為發生的時間戳記
 * @param occurredOn   事件產生的時間 (系統發布此事件的時間)
 */
public record BehaviorLoggedEvent(
        String behaviorId,
        String userId,
        String sessionId,
        String itemId,
        String behaviorType,
        String clientIp,
        String userAgent,
        String referrerUrl,
        Map<String, String> metadata,
        Instant timestamp,
        Instant occurredOn
) {

    /**
     * 從 UserBehavior 聚合根建立對應事件的便捷建構子
     *
     * @param behavior 聚合根實體
     */
    public BehaviorLoggedEvent(UserBehavior behavior) {
        this(
                behavior.getBehaviorId(),
                behavior.getUserId() != null ? behavior.getUserId().value() : null,
                behavior.getSessionId() != null ? behavior.getSessionId().value() : null,
                behavior.getItemId() != null ? behavior.getItemId().value() : null,
                behavior.getBehaviorType().name(),
                behavior.getDeviceInfo() != null ? behavior.getDeviceInfo().clientIp() : null,
                behavior.getDeviceInfo() != null ? behavior.getDeviceInfo().userAgent() : null,
                behavior.getReferrerUrl(),
                behavior.getMetadata() != null ? behavior.getMetadata().data() : null,
                behavior.getTimestamp(),
                Instant.now()
        );
    }
}
