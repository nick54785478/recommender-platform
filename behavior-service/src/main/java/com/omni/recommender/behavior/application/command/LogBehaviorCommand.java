package com.omni.recommender.behavior.application.command;

import java.time.Instant;
import java.util.Map;

/**
 * 記錄用戶行為命令 (Command)
 * 使用 Java Record 確保命令的純粹資料載體特性 (Data Carrier)，且不可變。
 */
public record LogBehaviorCommand(
        String userId,
        String sessionId,
        String itemId,
        String behaviorType,
        String clientIp,
        String userAgent,
        String referrerUrl,
        Map<String, String> metadata,
        Instant timestamp
) {}
