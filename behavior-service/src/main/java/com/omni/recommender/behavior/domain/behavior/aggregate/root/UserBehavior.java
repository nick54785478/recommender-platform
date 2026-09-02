package com.omni.recommender.behavior.domain.behavior.aggregate.root;

import com.omni.recommender.behavior.domain.behavior.aggregate.vo.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 用戶行為 (Aggregate Root)
 * 負責封裝單一用戶行為紀錄，並確保狀態與業務規則的一致性。
 * 遵守純領域模型 (Pure Domain Model) 設計規範，不允許依賴任何外部框架 (如 Lombok)。
 */
public class UserBehavior {
    
    // ==========================================
    // 1. 核心識別與關聯 (Identity & Relations)
    // ==========================================
    /** 系統內部唯一識別碼，用於唯一追蹤單一行為紀錄 */
    private final String behaviorId;
    
    /** 觸發此行為的會員 ID。若未登入，可為 null */
    private final UserId userId;
    
    /** 追蹤同一次的瀏覽/工作階段，對計算轉換漏斗、短期意圖與停留時間至關重要 */
    private final SessionId sessionId;
    
    /** 該行為所關聯的商品、行程或頁面 ID */
    private final ItemId itemId;

    // ==========================================
    // 2. 行為本質 (Behavior Core)
    // ==========================================
    /** 行為的本質類型 (例如：VIEW, CLICK, SEARCH, ADD_TO_CART, PURCHASE) */
    private final BehaviorType behaviorType;
    
    /** 行為發生的精確時間 */
    private final Instant timestamp;

    // ==========================================
    // 3. 裝置與環境上下文 (Context / Environment)
    // ==========================================
    /** 記錄發出請求的裝置環境 (如 IP 位址、User-Agent)，可用於進階推薦特徵與防詐欺 */
    private final DeviceInfo deviceInfo;
    
    /** 來源網址，記錄用戶是從哪裡點擊進入該頁面的 */
    private final String referrerUrl;

    // ==========================================
    // 4. 彈性擴充資料 (Metadata)
    // ==========================================
    /** 用於記錄特定行為才具備的資訊 (如 SEARCH 帶有 keyword)。不屬於通用的產業資料皆放置於此 */
    private final BehaviorContext metadata;

    /**
     * 私有建構子：僅允許透過 Factory Method 建立，確保 Aggregate 在建立時就符合領域不變量 (Invariants)。
     *
     * @param userId       用戶 ID
     * @param sessionId    工作階段 ID
     * @param itemId       商品 ID
     * @param behaviorType 行為類型
     * @param deviceInfo   裝置資訊
     * @param referrerUrl  來源網址
     * @param metadata     彈性擴充資料
     * @param timestamp    行為時間
     */
    private UserBehavior(UserId userId, SessionId sessionId, ItemId itemId, 
                         BehaviorType behaviorType, DeviceInfo deviceInfo, 
                         String referrerUrl, BehaviorContext metadata, Instant timestamp) {
        this.behaviorId = UUID.randomUUID().toString();
        this.userId = userId;
        this.sessionId = sessionId;
        this.itemId = itemId;
        this.behaviorType = behaviorType;
        this.deviceInfo = deviceInfo;
        this.referrerUrl = referrerUrl;
        
        // 若未提供 metadata，預設給予空物件以避免 NullPointerException
        this.metadata = metadata != null ? metadata : new BehaviorContext(null);
        
        // 若未提供時間，則系統自動賦予當下時間
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        
        // 執行領域規則驗證
        validateInvariants();
    }

    /**
     * 工廠方法 (Factory Method)，建立一筆新的用戶行為紀錄。
     *
     * @param userId       用戶 ID
     * @param sessionId    工作階段 ID
     * @param itemId       商品 ID
     * @param behaviorType 行為類型
     * @param deviceInfo   裝置資訊
     * @param referrerUrl  來源網址
     * @param metadata     彈性擴充資料
     * @param timestamp    行為時間
     * @return 建立完成且狀態正確的 UserBehavior 實體
     */
    public static UserBehavior log(UserId userId, SessionId sessionId, ItemId itemId, 
                                   BehaviorType behaviorType, DeviceInfo deviceInfo, 
                                   String referrerUrl, BehaviorContext metadata, Instant timestamp) {
        return new UserBehavior(userId, sessionId, itemId, behaviorType, deviceInfo, referrerUrl, metadata, timestamp);
    }

    /**
     * 驗證領域不變量 (Domain Invariants)。
     * 若違反業務規則，將會拋出例外阻斷建立過程。
     */
    private void validateInvariants() {
        if (this.behaviorType == BehaviorType.SEARCH && !this.metadata.hasKey("keyword")) {
            throw new IllegalArgumentException("SEARCH behavior must have a keyword in metadata");
        }
    }

    // ==========================================
    // 手動建立的 Getter 方法 (拒絕 Lombok 侵入)
    // ==========================================
    
    public String getBehaviorId() {
        return behaviorId;
    }

    public UserId getUserId() {
        return userId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public ItemId getItemId() {
        return itemId;
    }

    public BehaviorType getBehaviorType() {
        return behaviorType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public String getReferrerUrl() {
        return referrerUrl;
    }

    public BehaviorContext getMetadata() {
        return metadata;
    }
}
