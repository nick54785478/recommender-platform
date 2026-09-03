package com.omni.recommender.behavior.application.service;

import com.omni.recommender.behavior.application.command.LogBehaviorCommand;
import com.omni.recommender.behavior.application.port.in.GetRecentBehaviorsUseCase;
import com.omni.recommender.behavior.application.port.in.LogBehaviorUseCase;
import com.omni.recommender.behavior.application.port.out.BehaviorHBasePort;
import com.omni.recommender.behavior.application.port.out.BehaviorLocalLoggerPort;
import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import com.omni.recommender.behavior.domain.behavior.aggregate.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用戶行為 Application Service (Inbound Adapter)
 */
@Service
@RequiredArgsConstructor
public class BehaviorApplicationService implements LogBehaviorUseCase, GetRecentBehaviorsUseCase {

    private final BehaviorLocalLoggerPort behaviorLocalLoggerPort;
    private final BehaviorHBasePort behaviorHBasePort;

    @Override
    public void execute(LogBehaviorCommand command) {
        // 1. 轉換 Command 為 Domain Value Objects
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
        
        // 3. Dual-Write: 寫入本地日誌 (給 HDFS Batch 用)
        behaviorLocalLoggerPort.writeLog(behavior);
        
        // 4. Dual-Write: 寫入 HBase (給即時儀表板展示用)
        behaviorHBasePort.writeBehavior(behavior);
    }
    
    public List<UserBehavior> getRecentBehaviors(String userId, int limit) {
        return behaviorHBasePort.getRecentBehaviors(userId, limit);
    }
}
