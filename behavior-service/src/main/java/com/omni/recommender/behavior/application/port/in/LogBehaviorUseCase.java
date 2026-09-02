package com.omni.recommender.behavior.application.port.in;

import com.omni.recommender.behavior.application.command.LogBehaviorCommand;

/**
 * 記錄用戶行為 UseCase (Inbound Port)
 */
public interface LogBehaviorUseCase {
    void execute(LogBehaviorCommand command);
}
