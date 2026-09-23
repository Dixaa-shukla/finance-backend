package com.finance_backend.goal.service;

import com.finance_backend.goal.dto.GoalContributionRequest;
import com.finance_backend.goal.dto.GoalRequest;
import com.finance_backend.goal.dto.GoalResponse;
import com.finance_backend.goal.entity.FinancialGoal;
import com.finance_backend.goal.entity.GoalStatus;
import com.finance_backend.goal.exception.GoalNotFoundException;
import com.finance_backend.goal.repository.FinancialGoalRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class GoalServiceImpl implements GoalService {

    private final FinancialGoalRepository goalRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public GoalServiceImpl(FinancialGoalRepository goalRepository,
                           ResourceOwnershipGuard ownershipGuard) {
        this.goalRepository = goalRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        FinancialGoal goal = toEntity(request, new FinancialGoal());
        FinancialGoal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id) {
        FinancialGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));
        ownershipGuard.check(goal.getUserId(), "goal", id);
        return toResponse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByUserId(Long userId) {
        return goalRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GoalResponse contributeToGoal(Long id, GoalContributionRequest request) {
        FinancialGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));
        ownershipGuard.check(goal.getUserId(), "goal", id);

        goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
        FinancialGoal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long id, GoalRequest request) {
        FinancialGoal existing = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));
        ownershipGuard.check(existing.getUserId(), "goal", id);

        // currentAmount is intentionally untouched here -- only contributeToGoal changes it.
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setTargetAmount(request.getTargetAmount());
        existing.setTargetDate(request.getTargetDate());

        FinancialGoal saved = goalRepository.save(existing);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGoal(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        FinancialGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));
        ownershipGuard.check(goal.getUserId(), "goal", id);
        goalRepository.delete(goal);
    }

    // ---------- helpers ----------

    private FinancialGoal toEntity(GoalRequest request, FinancialGoal target) {
        target.setUserId(request.getUserId());
        target.setTitle(request.getTitle());
        target.setDescription(request.getDescription());
        target.setTargetAmount(request.getTargetAmount());
        target.setTargetDate(request.getTargetDate());
        return target;
    }

    private GoalResponse toResponse(FinancialGoal goal) {
        LocalDate today = LocalDate.now();
        BigDecimal remainingAmount = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        long daysRemaining = ChronoUnit.DAYS.between(today, goal.getTargetDate());

        double progressPercent = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        GoalStatus status;
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            status = GoalStatus.COMPLETED;
        } else if (today.isAfter(goal.getTargetDate())) {
            status = GoalStatus.EXPIRED;
        } else {
            status = GoalStatus.IN_PROGRESS;
        }

        BigDecimal suggestedMonthlySaving = null;
        if (status == GoalStatus.IN_PROGRESS && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            int monthsRemaining = Math.max(1, Period.between(today, goal.getTargetDate()).getMonths()
                    + (Period.between(today, goal.getTargetDate()).getYears() * 12) + 1);
            suggestedMonthlySaving = remainingAmount.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_UP);
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .userId(goal.getUserId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .remainingAmount(remainingAmount)
                .progressPercent(progressPercent)
                .targetDate(goal.getTargetDate())
                .daysRemaining(daysRemaining)
                .status(status)
                .suggestedMonthlySaving(suggestedMonthlySaving)
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }
}
