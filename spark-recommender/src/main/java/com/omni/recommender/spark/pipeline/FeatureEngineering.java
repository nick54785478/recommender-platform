package com.omni.recommender.spark.pipeline;

import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import lombok.extern.slf4j.Slf4j;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.when;

/**
 * 負責特徵工程與權重轉換 (Feature Engineering)
 */
@Slf4j
public class FeatureEngineering {

    private StringIndexerModel userIndexerModel;
    private StringIndexerModel itemIndexerModel;

    /**
     * 將原始日誌轉換為 ALS 模型可接受的格式 (數值 ID 與評分)
     */
    public Dataset<Row> transform(Dataset<Row> rawData) {
        log.info("Starting feature engineering (Mapping implicit feedbacks and generating numeric IDs)...");

        // 1. 將 behaviorType 轉換為隱式評分 (rating)
        Dataset<Row> scoredData = rawData.withColumn("rating",
                when(col("behaviorType").equalTo("VIEW"), 1.0)
                .when(col("behaviorType").equalTo("CLICK"), 2.0)
                .when(col("behaviorType").equalTo("SEARCH"), 2.5)
                .when(col("behaviorType").equalTo("LIKE"), 3.0)
                .when(col("behaviorType").equalTo("ADD_TO_CART"), 4.0)
                .when(col("behaviorType").equalTo("PURCHASE"), 5.0)
                .otherwise(0.0)
        );

        // 2. ALS 演算法要求 userCol 與 itemCol 必須是整數型別，但我們的 ID 是 String (UUID)。
        // 這裡使用 StringIndexer 將 String ID 轉換為 Double 數值 ID。
        StringIndexer userIndexer = new StringIndexer()
                .setInputCol("userId")
                .setOutputCol("userIdNum")
                .setHandleInvalid("skip"); // 忽略無效值 (如未登入用戶)
        
        this.userIndexerModel = userIndexer.fit(scoredData);
        Dataset<Row> userIndexed = userIndexerModel.transform(scoredData);

        StringIndexer itemIndexer = new StringIndexer()
                .setInputCol("itemId")
                .setOutputCol("itemIdNum")
                .setHandleInvalid("skip");
                
        this.itemIndexerModel = itemIndexer.fit(userIndexed);
        Dataset<Row> finalData = itemIndexerModel.transform(userIndexed);

        // 3. 為了後續寫入 HBase 時能還原字串 ID，我們保留原始的字串欄位
        return finalData.select("userId", "userIdNum", "itemId", "itemIdNum", "rating");
    }

    public StringIndexerModel getUserIndexerModel() {
        return userIndexerModel;
    }

    public StringIndexerModel getItemIndexerModel() {
        return itemIndexerModel;
    }
}
