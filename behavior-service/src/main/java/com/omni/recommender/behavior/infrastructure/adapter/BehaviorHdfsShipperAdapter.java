package com.omni.recommender.behavior.infrastructure.adapter;

import com.omni.recommender.behavior.application.port.out.BehaviorHdfsShipperPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * HDFS 日誌搬運工 (Outbound Adapter)
 * (依據規範：Adapter 必須位於 infrastructure/adapter/ 套件，且為 package-private)
 */
@Slf4j
@Component
class BehaviorHdfsShipperAdapter implements BehaviorHdfsShipperPort {

    // HDFS NameNode 的連線位址，由 application.yml 注入
    @Value("${hdfs.defaultFS}")
    private String hdfsUri;

    // 本地日誌存放的目錄，也是 Logback 輸出日誌的地方
    @Value("${behavior.log.dir:logs/behavior}")
    private String localLogDir;

    // HDFS 上用來存放原始用戶行為日誌的基礎路徑
    @Value("${hdfs.base.path:/data/raw/user_behavior}")
    private String hdfsBasePath;

    @Override
    public void shipLocalLogsToHdfs() {
        // --- 1. 檢查本地日誌目錄是否存在 ---
        File dir = new File(localLogDir);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Local log directory {} does not exist.", localLogDir);
            return;
        }

        // --- 2. 掃描準備上傳的日誌檔案 ---
        // 在 logback-spring.xml 中，我們設定了 TimeBasedRollingPolicy 會把舊檔案壓縮成 .gz 結尾。
        // 我們只抓取已經 roll (滾動) 完成的 .gz 壓縮檔，避免讀取到當前正在寫入的 .log 檔 (防鎖死與資料不全)。
        File[] rolledFiles = dir.listFiles((d, name) -> name.endsWith(".gz"));
        if (rolledFiles == null || rolledFiles.length == 0) {
            log.debug("No rolled log files found to ship.");
            return; // 沒有檔案需要上傳，直接結束
        }

        try {
            // --- 3. 建立 Hadoop 連線配置 ---
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", hdfsUri);
            
            // 解決 Windows 本地端直連 Docker HDFS 讀取 DataNode 時的 BlockMissingException (IP 路由問題)
            conf.set("dfs.client.use.datanode.hostname", "true");
            
            // 強制將 Hadoop 客戶端的執行身分設為 root (或具有寫入 HDFS 權限的使用者)
            // 這是為了解決本地開發機 (Windows/Mac) 使用者與 HDFS 伺服器使用者名稱不符導致的 Permission Denied 錯誤。
            System.setProperty("HADOOP_USER_NAME", "root"); 
            
            // 透過 try-with-resources 自動關閉 Hadoop FileSystem 連線，避免資源洩漏
            try (FileSystem fs = FileSystem.get(conf)) {
                
                // --- 4. 準備 HDFS 目標路徑 (依照日期分區 Partitioning) ---
                // 在大數據領域，資料通常會以日期 (yyyy-MM-dd) 為分區 (Partition)，以加速後續 Spark/Hive 的查詢效能。
                String dateFolder = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                Path hdfsTargetDir = new Path(hdfsBasePath + "/" + dateFolder);
                
                // 如果 HDFS 上的該日期資料夾不存在，就建立它
                if (!fs.exists(hdfsTargetDir)) {
                    fs.mkdirs(hdfsTargetDir);
                }

                // --- 5. 逐一上傳並清理本地檔案 ---
                for (File file : rolledFiles) {
                    Path localPath = new Path(file.getAbsolutePath());
                    Path hdfsPath = new Path(hdfsTargetDir, file.getName());
                    
                    log.info("Shipping file {} to HDFS {}", localPath, hdfsPath);
                    
                    // 執行 HDFS 上傳指令。
                    // 參數1 (delSrc): true 代表上傳成功後，自動刪除本地端的原始檔案，以節省本地硬碟空間。
                    // 參數2 (overwrite): true 代表如果 HDFS 遠端已經有同名檔案，則直接覆蓋。
                    fs.copyFromLocalFile(true, true, localPath, hdfsPath);
                    
                    log.info("Successfully shipped and deleted local file: {}", file.getName());
                }
            }
        } catch (IOException e) {
            // 捕捉網路連線失敗或 IO 錯誤，記錄下來等待下次 Scheduler 重新觸發。
            log.error("Failed to ship logs to HDFS", e);
        }
    }
}
