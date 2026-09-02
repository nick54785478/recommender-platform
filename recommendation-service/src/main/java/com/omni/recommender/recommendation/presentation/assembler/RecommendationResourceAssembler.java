package com.omni.recommender.recommendation.presentation.assembler;

import com.omni.recommender.recommendation.application.view.RecommendationGottenView;
import com.omni.recommender.recommendation.presentation.resource.out.RecommendationGottenResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 負責將 Application View 轉換為 Presentation Resource
 */
@Component
public class RecommendationResourceAssembler {

    public RecommendationGottenResource toResource(RecommendationGottenView view) {
        RecommendationGottenResource resource = new RecommendationGottenResource();
        resource.setUserId(view.userId());
        resource.setGeneratedAt(view.generatedAt());
        resource.setFallback(view.isFallback());

        List<RecommendationGottenResource.ItemResource> itemResources = view.items().stream()
                .map(item -> {
                    RecommendationGottenResource.ItemResource itemResource = new RecommendationGottenResource.ItemResource();
                    itemResource.setItemId(item.itemId());
                    itemResource.setScore(item.score());
                    itemResource.setRank(item.rank());
                    return itemResource;
                })
                .collect(Collectors.toList());

        resource.setItems(itemResources);
        return resource;
    }
}
