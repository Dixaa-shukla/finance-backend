package com.finance_backend.goal.service;

import com.finance_backend.goal.dto.GoalContributionRequest;
import com.finance_backend.goal.dto.GoalRequest;
import com.finance_backend.goal.dto.GoalResponse;

import java.util.List;

public interface GoalService {

    GoalResponse createGoal(GoalRequest request);

    GoalResponse getGoalById(Long id);

    List<GoalResponse> getGoalsByUserId(Long userId);

    /** Adds the given amount to the goal's saved-so-far total. */
    GoalResponse contributeToGoal(Long id, GoalContributionRequest request);

    GoalResponse updateGoal(Long id, GoalRequest request);

    void deleteGoal(Long id);
}

