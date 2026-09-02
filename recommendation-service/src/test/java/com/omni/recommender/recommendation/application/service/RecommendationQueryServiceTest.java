package com.omni.recommender.recommendation.application.service;

import com.omni.recommender.recommendation.application.port.out.RecommendationHBasePort;
import com.omni.recommender.recommendation.application.query.GetRecommendationQuery;
import com.omni.recommender.recommendation.application.view.RecommendationGottenView;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.root.Recommendation;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.RecommendedItem;
import com.omni.recommender.recommendation.domain.recommendation.aggregate.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RecommendationQueryServiceTest {

    private RecommendationHBasePort hbasePort;
    private RecommendationQueryService queryService;

    @BeforeEach
    void setUp() {
        hbasePort = Mockito.mock(RecommendationHBasePort.class);
        queryService = new RecommendationQueryService(hbasePort);
    }

    @Test
    void shouldReturnRecommendationWhenFoundInHBase() {
        // Arrange
        UserId userId = new UserId("U2001");
        List<RecommendedItem> items = List.of(
                new RecommendedItem("ITEM_A", 0.95, 1),
                new RecommendedItem("ITEM_B", 0.90, 2)
        );
        Recommendation mockRecommendation = Recommendation.restore(userId, items, Instant.now());
        when(hbasePort.fetchRecommendation(any(UserId.class))).thenReturn(Optional.of(mockRecommendation));

        GetRecommendationQuery query = new GetRecommendationQuery("U2001", 5);

        // Act
        RecommendationGottenView view = queryService.execute(query);

        // Assert
        assertEquals("U2001", view.userId());
        assertFalse(view.isFallback());
        assertEquals(2, view.items().size());
        assertEquals("ITEM_A", view.items().get(0).itemId());
    }

    @Test
    void shouldReturnFallbackWhenHBaseMisses() {
        // Arrange (模擬 Cache Miss)
        when(hbasePort.fetchRecommendation(any(UserId.class))).thenReturn(Optional.empty());
        GetRecommendationQuery query = new GetRecommendationQuery("U9999", 2);

        // Act
        RecommendationGottenView view = queryService.execute(query);

        // Assert
        assertEquals("U9999", view.userId());
        assertTrue(view.isFallback()); // 確認啟動了降級策略
        assertEquals(2, view.items().size()); // 雖然 fallback 給了三筆，但 query.limit 是 2
        assertEquals("TPE-NRT-PROMO", view.items().getFirst().itemId()); // 驗證是否拿到預設熱門項目
    }
}
