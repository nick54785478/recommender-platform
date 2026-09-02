package com.omni.recommender.behavior.presentation.resource.in;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

/**
 * 接收記錄行為請求的 Resource
 * Naming Rule: V + N + Resource
 */
@Data
public class LogBehaviorResource {
    
    private String userId; // 未登入可能為空
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    private String itemId;
    
    @NotBlank(message = "Behavior type is required")
    private String behaviorType;

    private String referrerUrl;
    
    private Map<String, String> metadata;
}
