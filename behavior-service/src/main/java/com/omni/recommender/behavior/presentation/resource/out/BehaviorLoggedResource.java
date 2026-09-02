package com.omni.recommender.behavior.presentation.resource.out;

import lombok.Builder;
import lombok.Data;

/**
 * 回傳記錄完成的 Resource
 * Naming Rule: N + Ved + Resource
 */
@Data
@Builder
public class BehaviorLoggedResource {
    private String status;
    private String message;
}
