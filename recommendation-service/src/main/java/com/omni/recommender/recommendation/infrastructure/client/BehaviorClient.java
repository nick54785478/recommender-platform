package com.omni.recommender.recommendation.infrastructure.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class BehaviorClient {

    private final RestTemplate restTemplate;

    public BehaviorClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 從 behavior-service 取得使用者近期的行為紀錄
     */
    public List<Map<String, Object>> getRecentBehaviors(String userId) {
        try {
            // 在 docker-compose 環境中可以使用 http://behavior-service:9001
            // 但因為我們目前是在本地端運行 POC，所以先寫死 localhost:9001
            String url = "http://localhost:9001/api/v1/behaviors/" + userId;
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch behaviors from behavior-service: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
