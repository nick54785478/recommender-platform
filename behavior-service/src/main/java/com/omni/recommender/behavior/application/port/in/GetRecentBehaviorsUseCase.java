package com.omni.recommender.behavior.application.port.in;

import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import java.util.List;

public interface GetRecentBehaviorsUseCase {
    List<UserBehavior> getRecentBehaviors(String userId, int limit);
}
