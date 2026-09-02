package com.omni.recommender.spark.model;

import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責訓練 ALS 協同過濾模型
 */
@Slf4j
public class AlsRecommenderTrainer {
    
    /**
     * @param trainingData 經過轉換包含 userIdNum, itemIdNum, rating 的 DataFrame
     * @param numRecommendations 針對每位用戶產出的最大推薦筆數 (Top-N)
     * @return 包含 userIdNum 與 recommendations(Array) 的 DataFrame
     */
    public Dataset<Row> trainAndRecommend(Dataset<Row> trainingData, int numRecommendations) {
        log.info("Training ALS Model with implicit preferences...");
        
        ALS als = new ALS()
                .setMaxIter(10)
                .setRegParam(0.01)
                // 核心：啟用隱式回饋 (Implicit Feedback)，因為我們是以用戶點擊、購買等行為做為權重，而非直接評分
                .setImplicitPrefs(true) 
                .setUserCol("userIdNum")
                .setItemCol("itemIdNum")
                .setRatingCol("rating")
                // 處理冷啟動問題：當預測時遇到沒見過的 User/Item 時直接捨棄，避免拋出 NaN
                .setColdStartStrategy("drop");
                
        ALSModel model = als.fit(trainingData);
        log.info("Model training completed. Generating top {} recommendations for all users...", numRecommendations);
        
        return model.recommendForAllUsers(numRecommendations);
    }
}
