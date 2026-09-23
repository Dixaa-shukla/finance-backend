package com.finance_backend.ai.financialAssistant.service;

import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.service.BudgetService;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.goal.dto.GoalResponse;
import com.finance_backend.goal.service.GoalService;
import com.finance_backend.investmentTracker.dto.InvestmentSummaryResponse;
import com.finance_backend.investmentTracker.service.InvestmentService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gathers a compact, plain-text financial summary for a user by calling
 * the EXISTING service layers of Expense, Budget, Goal, and Investment
 */
@Component
public class FinancialContextBuilder {

    private final ExpenseService expenseService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final InvestmentService investmentService;

    public FinancialContextBuilder(ExpenseService expenseService,
                                   BudgetService budgetService,
                                   GoalService goalService,
                                   InvestmentService investmentService) {
        this.expenseService = expenseService;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.investmentService = investmentService;
    }

    public String buildContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== User's Financial Snapshot ===\n\n");
        appendExpenseSummary(sb, userId);
        appendBudgetSummary(sb, userId);
        appendGoalSummary(sb, userId);
        appendInvestmentSummary(sb, userId);
        return sb.toString();
    }

    private void appendExpenseSummary(StringBuilder sb, Long userId) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByUserId(userId);
        LocalDate monthStart = YearMonth.from(LocalDate.now()).atDay(1);

        List<ExpenseResponse> thisMonth = expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(monthStart))
                .toList();

        BigDecimal totalThisMonth = thisMonth.stream()
                .map(ExpenseResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = thisMonth.stream()
                .collect(Collectors.groupingBy(ExpenseResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

        sb.append("Expenses this month: total Rs. ").append(totalThisMonth).append("\n");
        if (!byCategory.isEmpty()) {
            sb.append("By category: ");
            byCategory.forEach((cat, amt) -> sb.append(cat).append("=Rs.").append(amt).append("; "));
            sb.append("\n");
        }
        sb.append("\n");
    }

    private void appendBudgetSummary(StringBuilder sb, Long userId) {
        List<BudgetResponse> budgets = budgetService.getBudgetsByUserId(userId);
        if (budgets.isEmpty()) {
            sb.append("Budgets: none set.\n\n");
            return;
        }
        sb.append("Budgets:\n");
        for (BudgetResponse b : budgets) {
            String label = b.getCategoryName() != null ? b.getCategoryName() : "Overall";
            sb.append("- ").append(label).append(" (").append(b.getPeriod()).append("): limit Rs.")
                    .append(b.getAmount()).append(", spent Rs.").append(b.getSpentAmount())
                    .append(String.format(" (%.1f%% used)", b.getPercentUsed()))
                    .append(b.isAlertTriggered() ? " [ALERT: over threshold]" : "")
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendGoalSummary(StringBuilder sb, Long userId) {
        List<GoalResponse> goals = goalService.getGoalsByUserId(userId);
        if (goals.isEmpty()) {
            sb.append("Financial goals: none set.\n\n");
            return;
        }
        sb.append("Financial goals:\n");
        for (GoalResponse g : goals) {
            sb.append("- ").append(g.getTitle()).append(": Rs.").append(g.getCurrentAmount())
                    .append(" of Rs.").append(g.getTargetAmount())
                    .append(String.format(" (%.1f%% complete)", g.getProgressPercent()))
                    .append(", status=").append(g.getStatus())
                    .append(", target date=").append(g.getTargetDate())
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendInvestmentSummary(StringBuilder sb, Long userId) {
        InvestmentSummaryResponse summary = investmentService.getPortfolioSummary(userId);
        if (summary.getTotalInvestmentCount() == 0) {
            sb.append("Investments: none recorded.\n");
            return;
        }
        sb.append("Investment portfolio: invested Rs.").append(summary.getTotalInvested())
                .append(", current value Rs.").append(summary.getTotalCurrentValue())
                .append(String.format(" (%.1f%% overall gain/loss)", summary.getTotalGainLossPercent()))
                .append("\n");
    }
}
