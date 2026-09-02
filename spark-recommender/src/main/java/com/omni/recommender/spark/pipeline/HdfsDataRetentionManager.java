package com.omni.recommender.spark.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * HDFS 資料生命週期管理 (Data Retention)
 * 負責掃描並刪除超過指定天數的過期資料，避免 HDFS 空間被塞爆。
 */
@Slf4j
public class HdfsDataRetentionManager {

    /**
     * 清理過期的 HDFS 資料分區 (以 yyyy-MM-dd 資料夾命名為準)
     *
     * @param spark         SparkSession (用來取得 Hadoop Configuration)
     * @param basePathUri   要掃描的 HDFS 基礎路徑 (例如: hdfs://namenode:8020/data/raw/user_behavior)
     * @param retentionDays 保留天數 (超過此天數的資料夾將被刪除)
     */
    public void cleanupOldData(SparkSession spark, String basePathUri, int retentionDays) {
        log.info("Starting HDFS data retention cleanup. Path: {}, Retention: {} days", basePathUri, retentionDays);
        
        LocalDate today = LocalDate.now();
        Path basePath = new Path(basePathUri);

        try (FileSystem fs = basePath.getFileSystem(spark.sparkContext().hadoopConfiguration())) {
            
            if (!fs.exists(basePath)) {
                log.warn("Base path {} does not exist on HDFS. Skipping cleanup.", basePathUri);
                return;
            }

            FileStatus[] statuses = fs.listStatus(basePath);
            int deletedCount = 0;

            for (FileStatus status : statuses) {
                if (status.isDirectory()) {
                    String folderName = status.getPath().getName();
                    
                    try {
                        // 解析資料夾名稱為日期 (預期格式: yyyy-MM-dd)
                        LocalDate folderDate = LocalDate.parse(folderName);
                        
                        // 計算該資料夾距離今天已經過了幾天
                        long daysBetween = ChronoUnit.DAYS.between(folderDate, today);
                        
                        if (daysBetween > retentionDays) {
                            log.info("Deleting expired HDFS partition: {} ({} days old)", status.getPath(), daysBetween);
                            // 遞迴刪除資料夾 (參數2 = true)
                            boolean success = fs.delete(status.getPath(), true);
                            if (success) {
                                deletedCount++;
                            } else {
                                log.error("Failed to delete HDFS path: {}", status.getPath());
                            }
                        }
                    } catch (DateTimeParseException e) {
                        // 如果資料夾名稱不是日期格式，就跳過不處理
                        log.debug("Skipping non-date folder: {}", folderName);
                    }
                }
            }
            
            log.info("HDFS cleanup finished. Deleted {} expired partitions.", deletedCount);
            
        } catch (IOException e) {
            log.error("Failed to perform HDFS cleanup due to IO error", e);
        }
    }
}
