package com.omni.recommender.spark.pipeline;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責從 HDFS 讀取原始行為日誌 (Data Ingestion)
 */
@Slf4j
public class DataIngestionPipeline {
    
    /**
     * 從指定路徑載入 JSON 日誌檔案
     * @param spark SparkSession
     * @param hdfsPath HDFS 日誌路徑 (例如: hdfs://namenode:8020/data/raw/user_behavior/2023-10-01/*.gz)
     * @return 結構化的 {@code Dataset<Row>}
     */
    public Dataset<Row> loadRawLogs(SparkSession spark, String hdfsPath) {
        log.info("Loading raw logs from HDFS path: {}", hdfsPath);
        
        // Spark 原生支援讀取 JSONL (一行一筆 JSON) 以及 .gz 壓縮格式
        return spark.read().json(hdfsPath);
    }
}
