package com.finance_backend.goal.service;

import com.finance_backend.auth.security.ResourceOwnershipGuard;
import com.finance_backend.goal.dto.GoalContributionRequest;
import com.finance_backend.goal.entity.FinancialGoal;
import com.finance_backend.goal.entity.GoalStatus;
import com.finance_backend.goal.repository.FinancialGoalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock private FinancialGoalRepository goalRepository;
    @Mock private ResourceOwnershipGuard ownershipGuard;
    @InjectMocks private GoalServiceImpl goalService;

    @Test
    void marksAReachedGoalAsCompleted() {
        // Arrange
        FinancialGoal goal = goal(new BigDecimal("100"), new BigDecimal("100"), LocalDate.now().plusDays(10));
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        // Act
        var response = goalService.getGoalById(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(response.getSuggestedMonthlySaving()).isNull();
        verify(ownershipGuard).check(7L, "goal", 1L);
    }

    @Test
    void marksAnUnreachedPastGoalAsExpired() {
        // Arrange
        FinancialGoal goal = goal(new BigDecimal("100"), new BigDecimal("25"), LocalDate.now().minusDays(1));
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        // Act
        var response = goalService.getGoalById(1L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(GoalStatus.EXPIRED);
        assertThat(response.getSuggestedMonthlySaving()).isNull();
    }

    @Test
    void addsAContributionToTheExistingBalance() {
        // Arrange
        FinancialGoal goal = goal(new BigDecimal("100"), new BigDecimal("25"), LocalDate.now().plusMonths(2));
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(FinancialGoal.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        var response = goalService.contributeToGoal(1L,
                GoalContributionRequest.builder().amount(new BigDecimal("10")).build());

        // Assert
        assertThat(response.getCurrentAmount()).isEqualByComparingTo("35");
        verify(ownershipGuard).check(7L, "goal", 1L);
    }

    private FinancialGoal goal(BigDecimal target, BigDecimal current, LocalDate targetDate) {
        return FinancialGoal.builder().id(1L).userId(7L).title("Emergency fund")
                .targetAmount(target).currentAmount(current).targetDate(targetDate).build();
    }
}
