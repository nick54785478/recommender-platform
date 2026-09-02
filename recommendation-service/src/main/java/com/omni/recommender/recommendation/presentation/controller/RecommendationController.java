package com.omni.recommender.recommendation.presentation.controller;

import com.omni.recommender.recommendation.application.port.in.GetRecommendationUseCase;
import com.omni.recommender.recommendation.application.query.GetRecommendationQuery;
import com.omni.recommender.recommendation.application.view.RecommendationGottenView;
import com.omni.recommender.recommendation.presentation.assembler.RecommendationResourceAssembler;
import com.omni.recommender.recommendation.presentation.resource.out.RecommendationGottenResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 推薦服務 REST API 介面
 * 宣告為 package-private 以符合 Adapter 規範 (Spring Framework 可支援 package-private 掃描)。
 */
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
class RecommendationController {

    private final GetRecommendationUseCase getRecommendationUseCase;
    private final RecommendationResourceAssembler assembler;

    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationGottenResource> getRecommendation(
            @PathVariable("userId") String userId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        // 1. 組裝 Query
        GetRecommendationQuery query = new GetRecommendationQuery(userId, limit);

        // 2. 呼叫 Application UseCase 進行查詢
        RecommendationGottenView view = getRecommendationUseCase.execute(query);

        // 3. 轉換為對外 Resource 並回傳
        RecommendationGottenResource resource = assembler.toResource(view);
        return ResponseEntity.ok(resource);
    }
}
