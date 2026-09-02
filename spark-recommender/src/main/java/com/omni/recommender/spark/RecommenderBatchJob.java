package com.omni.recommender.spark;

import com.omni.recommender.spark.model.AlsRecommenderTrainer;
import com.omni.recommender.spark.pipeline.DataIngestionPipeline;
import com.omni.recommender.spark.pipeline.FeatureEngineering;
import com.omni.recommender.spark.pipeline.HdfsDataRetentionManager;
import com.omni.recommender.spark.sink.HBaseWriter;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Spark 推薦模型批次作業入口 (Batch Job Entry Point)
 * 此程式會被包裝成 Fat JAR，提交至 YARN 或獨立的 Spark Cluster 上執行。
 */
@Slf4j
public class RecommenderBatchJob {

    public static void main(String[] args) {
        log.info("Starting OmniRecommender Spark Batch Scheduler...");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // 排程設定：啟動時立刻執行一次 (initialDelay = 0)，之後每 15 分鐘執行一次
        scheduler.scheduleAtFixedRate(RecommenderBatchJob::runBatchJob, 0, 15, TimeUnit.MINUTES);
        
        // 保持 main thread 存活，不然程式會直接結束
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.error("Batch Job Scheduler interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void runBatchJob() {
        log.info("==================================================");
        log.info("Starting scheduled ALS Model Training Batch Job...");
        log.info("==================================================");

        // 針對 Windows 開發環境的繞過機制 (避免 winutils.exe FileNotFoundException)
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            if (System.getProperty("hadoop.home.dir") == null && System.getenv("HADOOP_HOME") == null) {
                System.setProperty("hadoop.home.dir", new java.io.File(".").getAbsolutePath());
                log.warn("Running on Windows without HADOOP_HOME. Bypassing winutils check...");
            }
        }

        // 1. 初始化 SparkSession
        SparkSession.Builder builder = SparkSession.builder()
                .appName("OmniRecommender-ALS-Job");

        // 自動防呆機制：如果沒有設定 Master (代表是你在 IntelliJ 直接按 Run)，就自動補上 local[*]
        if (System.getProperty("spark.master") == null) {
            log.info("No spark.master detected. Defaulting to local[*] for IDE execution.");
            builder.master("local[*]");
        }

        SparkSession spark = builder.getOrCreate();

        // 解決 Windows 本地端直連 Docker HDFS 讀取 DataNode 時的 BlockMissingException (IP 路由問題)
        spark.sparkContext().hadoopConfiguration().set("dfs.client.use.datanode.hostname", "true");

        try {
            // 1.5 基礎設施維護: 執行 HDFS 資料生命週期管理 (清理 30 天前的舊日誌，避免硬碟塞爆)
            HdfsDataRetentionManager retentionManager = new HdfsDataRetentionManager();
            retentionManager.cleanupOldData(spark, "hdfs://namenode:8020/data/raw/user_behavior", 30);

            // 2. Data Ingestion: 從 HDFS 讀取累積的原始行為 JSON 日誌
            DataIngestionPipeline ingestion = new DataIngestionPipeline();
            Dataset<Row> rawData = ingestion.loadRawLogs(spark, "hdfs://namenode:8020/data/raw/user_behavior/*/*");

            // 3. Feature Engineering: 轉換 behaviorType 權重與 String ID 為 Numeric ID
            FeatureEngineering fe = new FeatureEngineering();
            Dataset<Row> trainingData = fe.transform(rawData);

            // 4. Model Training: 訓練 ALS 協同過濾模型並產出 Top 10 推薦
            AlsRecommenderTrainer trainer = new AlsRecommenderTrainer();
            Dataset<Row> numericRecs = trainer.trainAndRecommend(trainingData, 10);
            
            // 5. Post-Processing: 將數值 ID 還原回原本的字串 UUID
            // 5.1 還原 userId
            IndexToString userIdConverter = new IndexToString()
                    .setInputCol("userIdNum")
                    .setOutputCol("userId")
                    .setLabels(fe.getUserIndexerModel().labels());
            Dataset<Row> recsWithUserId = userIdConverter.transform(numericRecs);

            // 5.2 拆解陣列、還原 itemId、並重新組裝回 Array 
            // 這個步驟是為了讓寫入 HBase 的格式可以直接被 Backend API 當成 JSON 返回
            Dataset<Row> exploded = recsWithUserId.withColumn("rec", functions.explode(functions.col("recommendations")))
                    .select(
                            functions.col("userId"),
                            functions.col("rec.itemIdNum").as("itemIdNum"),
                            functions.col("rec.rating").as("score")
                    );

            IndexToString itemIdConverter = new IndexToString()
                    .setInputCol("itemIdNum")
                    .setOutputCol("itemId")
                    .setLabels(fe.getItemIndexerModel().labels());
                    
            Dataset<Row> mappedItems = itemIdConverter.transform(exploded);

            // 重新群組化，並將陣列轉換為 JSON 字串
            Dataset<Row> grouped = mappedItems.groupBy("userId")
                    .agg(functions.collect_list(functions.struct("itemId", "score")).alias("items"));

            Dataset<Row> jsonRecs = grouped
                    .withColumn("itemsJson", functions.to_json(functions.col("items")))
                    .select("userId", "itemsJson");

            // 6. Sink to HBase: 寫入推薦結果供 recommendation-service 查詢
            // (zookeeper hostname 與 docker-compose.yml 一致)
            HBaseWriter writer = new HBaseWriter();
            writer.write(jsonRecs, "zookeeper");

            log.info("RecommenderBatchJob completed successfully.");
        } catch (Exception e) {
            log.error("Batch Job failed unexpectedly", e);
        } finally {
            spark.stop();
        }
    }
}
