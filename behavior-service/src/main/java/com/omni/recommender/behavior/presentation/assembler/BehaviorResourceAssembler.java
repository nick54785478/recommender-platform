package com.omni.recommender.behavior.presentation.assembler;

import com.omni.recommender.behavior.application.command.LogBehaviorCommand;
import com.omni.recommender.behavior.presentation.resource.in.LogBehaviorResource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

/**
 * 負責將表現層 Resource 轉換為應用層 Command
 */
@Component
public class BehaviorResourceAssembler {

    public LogBehaviorCommand toCommand(LogBehaviorResource resource) {
        
        // 嘗試從 HttpServletRequest 獲取 IP 與 UserAgent
        String clientIp = "unknown";
        String userAgent = "unknown";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            clientIp = request.getRemoteAddr();
            // 在實際應用中應檢查 X-Forwarded-For 標頭
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                clientIp = forwardedFor.split(",")[0].trim();
            }
            userAgent = request.getHeader("User-Agent");
        }

        // 使用 Record 建構子替換原本的 Builder
        return new LogBehaviorCommand(
                resource.getUserId(),
                resource.getSessionId(),
                resource.getItemId(),
                resource.getBehaviorType(),
                clientIp,
                userAgent,
                resource.getReferrerUrl(),
                resource.getMetadata(),
                Instant.now() // 由系統賦予接收時間
        );
    }
}
