package com.finance_backend.goal.controller;

import com.finance_backend.goal.dto.GoalContributionRequest;
import com.finance_backend.goal.dto.GoalRequest;
import com.finance_backend.goal.dto.GoalResponse;
import com.finance_backend.goal.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * Create a new financial goal.
     * POST /api/v1/goals
     */
    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.createGoal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a single goal by id, including computed progress/status/suggestion.
     * GET /api/v1/goals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoalById(id));
    }

    /**
     * All goals for a user.
     * GET /api/v1/goals/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GoalResponse>> getGoalsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(goalService.getGoalsByUserId(userId));
    }

    /**
     * Record a contribution towards a goal (adds to currentAmount).
     * POST /api/v1/goals/{id}/contribute
     */
    @PostMapping("/{id}/contribute")
    public ResponseEntity<GoalResponse> contributeToGoal(@PathVariable Long id,
                                                         @Valid @RequestBody GoalContributionRequest request) {
        return ResponseEntity.ok(goalService.contributeToGoal(id, request));
    }

    /**
     * Update a goal's title/description/targetAmount/targetDate.
     * Does NOT change currentAmount -- use the contribute endpoint for that.
     * PUT /api/v1/goals/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable Long id,
                                                   @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(id, request));
    }

    /**
     * Delete a goal.
     * DELETE /api/v1/goals/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
