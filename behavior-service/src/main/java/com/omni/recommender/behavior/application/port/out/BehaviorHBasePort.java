package com.omni.recommender.behavior.application.port.out;

import com.omni.recommender.behavior.domain.behavior.aggregate.root.UserBehavior;
import java.util.List;

public interface BehaviorHBasePort {
    void writeBehavior(UserBehavior behavior);
    List<UserBehavior> getRecentBehaviors(String userId, int limit);
}
