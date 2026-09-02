package com.omni.recommender.behavior.application.service;

import com.omni.recommender.behavior.application.command.LogBehaviorCommand;
import com.omni.recommender.behavior.application.port.in.LogBehaviorUseCase;
import com.omni.recommender.behavior.application.port.out.BehaviorLocalLoggerPort;
import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import com.omni.recommender.behavior.domain.behavior.aggregate.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用戶行為 Application Service (Inbound Adapter)
 */
@Service
@RequiredArgsConstructor
class BehaviorApplicationService implements LogBehaviorUseCase {

    private final BehaviorLocalLoggerPort behaviorLocalLoggerPort;

    @Override
    public void execute(LogBehaviorCommand command) {
        // 1. 轉換 Command 為 Domain Value Objects (使用 Record 原生存取方法 command.xxx())
        UserId userId = command.userId() != null ? new UserId(command.userId()) : null;
        SessionId sessionId = command.sessionId() != null ? new SessionId(command.sessionId()) : null;
        ItemId itemId = command.itemId() != null ? new ItemId(command.itemId()) : null;
        BehaviorType type = BehaviorType.valueOf(command.behaviorType().toUpperCase());
        DeviceInfo deviceInfo = new DeviceInfo(command.clientIp(), command.userAgent());
        BehaviorContext context = new BehaviorContext(command.metadata());
        
        // 2. 建立 Aggregate Root
        UserBehavior behavior = UserBehavior.log(
            userId, sessionId, itemId, type, deviceInfo, 
            command.referrerUrl(), context, command.timestamp()
        );
        
        // 3. 呼叫 Outbound Port，非同步寫入本地日誌
        behaviorLocalLoggerPort.writeLog(behavior);
    }
}
