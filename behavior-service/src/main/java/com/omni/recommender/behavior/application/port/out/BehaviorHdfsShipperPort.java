package com.omni.recommender.behavior.application.port.out;

/**
 * 【Outbound Port (Driven Port)】
 * 負責定義將本地已收集的行為日誌批次上傳 (Ship) 至 Hadoop 分散式檔案系統 (HDFS) 的抽象介面。
 * 
 * 依照 Clean Architecture 規範，此介面將業務邏輯 (如觸發上傳的時機) 與基礎設施細節 (如 Hadoop Client API) 隔離開來。
 */
public interface BehaviorHdfsShipperPort {
    
    /**
     * 執行批次上傳日誌任務。
     * 
     * 實作注意事項：
     * 實作端應負責掃描本地已完成滾動 (Rolled) 準備就緒的日誌檔案 (如 .log.gz)，
     * 將其安全地上傳至 HDFS 對應的日期資料夾中。
     * 必須確保在上傳成功後，清除或妥善封存本地來源檔案以釋放磁碟空間。
     */
    void shipLocalLogsToHdfs();
}
