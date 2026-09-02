package com.omni.recommender.recommendation.application.port.out;

import com.omni.recommender.recommendation.domain.recommendation.aggregate.root.Recommendation;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;

import java.util.Optional;

/**
 * 向底層 (HBase) 獲取資料的 Outbound Port
 * 遵循 ... + Port 命名規範。
 */
public interface RecommendationHBasePort {
    /**
     * 根據 UserId 獲取推薦清單
     * @param userId 用戶識別碼
     * @return 若 HBase 中有資料則回傳，否則回傳 empty 以觸發降級策略
     */
    Optional<Recommendation> fetchRecommendation(UserId userId);
}
