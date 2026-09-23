package com.finance_backend.dashboard.service;

import com.finance_backend.ai.spendingAnalytics.dto.HealthScoreResponse;
import com.finance_backend.ai.spendingAnalytics.service.AnalyticsService;
import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.service.BudgetService;
import com.finance_backend.dashboard.dto.BudgetOverviewResponse;
import com.finance_backend.dashboard.dto.ChartDataset;
import com.finance_backend.dashboard.dto.ChartGroupBy;
import com.finance_backend.dashboard.dto.ChartResponse;
import com.finance_backend.dashboard.dto.DashboardSummaryResponse;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.goal.dto.GoalResponse;
import com.finance_backend.goal.entity.GoalStatus;
import com.finance_backend.goal.service.GoalService;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.service.IncomeService;
import com.finance_backend.investmentTracker.dto.InvestmentSummaryResponse;
import com.finance_backend.investmentTracker.service.InvestmentService;
import com.finance_backend.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final InvestmentService investmentService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    public DashboardServiceImpl(ExpenseService expenseService,
                                IncomeService incomeService,
                                BudgetService budgetService,
                                GoalService goalService,
                                InvestmentService investmentService,
                                NotificationService notificationService,
                                AnalyticsService analyticsService) {
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.investmentService = investmentService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        YearMonth currentMonth = YearMonth.now();

        BigDecimal totalIncome = sumForMonth(incomeService.getIncomesByUserId(userId),
                IncomeResponse::getIncomeDate, IncomeResponse::getAmount, currentMonth);
        BigDecimal totalExpense = sumForMonth(expenseService.getExpensesByUserId(userId),
                ExpenseResponse::getExpenseDate, ExpenseResponse::getAmount, currentMonth);

        HealthScoreResponse healthScore = analyticsService.getHealthScore(userId);

        List<BudgetResponse> budgets = budgetService.getBudgetsByUserId(userId);
        long triggeredAlerts = budgets.stream().filter(BudgetResponse::isAlertTriggered).count();

        List<GoalResponse> goals = goalService.getGoalsByUserId(userId);
        long goalsNearingOrExpired = goals.stream()
                .filter(g -> g.getStatus() != GoalStatus.COMPLETED)
                .filter(g -> g.getStatus() == GoalStatus.EXPIRED || g.getDaysRemaining() <= 7)
                .count();

        InvestmentSummaryResponse investments = investmentService.getPortfolioSummary(userId);

        long unreadNotifications = notificationService.getUnreadCount(userId);

        return DashboardSummaryResponse.builder()
                .userId(userId)
                .totalIncomeThisMonth(totalIncome)
                .totalExpenseThisMonth(totalExpense)
                .netSavingsThisMonth(totalIncome.subtract(totalExpense))
                .healthScore(healthScore.getScore())
                .healthScoreLabel(healthScore.getLabel())
                .activeBudgetsCount(budgets.size())
                .triggeredBudgetAlertsCount((int) triggeredAlerts)
                .activeGoalsCount((int) goals.stream().filter(g -> g.getStatus() != GoalStatus.EXPIRED).count())
                .goalsNearingDeadlineOrExpiredCount((int) goalsNearingOrExpired)
                .totalInvestmentValue(investments.getTotalCurrentValue())
                .totalInvestmentGainLoss(investments.getTotalGainLossAmount())
                .unreadNotificationsCount(unreadNotifications)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChartResponse getExpenseChart(Long userId, ChartGroupBy groupBy, int months) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByUserId(userId);

        if (groupBy == ChartGroupBy.CATEGORY) {
            YearMonth currentMonth = YearMonth.now();
            Map<String, BigDecimal> byCategory = expenses.stream()
                    .filter(e -> YearMonth.from(e.getExpenseDate()).equals(currentMonth))
                    .collect(Collectors.groupingBy(ExpenseResponse::getCategory, LinkedHashMap::new,
                            Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

            return toSingleDatasetChart(byCategory, "Expenses");
        }

        Map<String, BigDecimal> byMonth = totalsByMonth(expenses, ExpenseResponse::getExpenseDate,
                ExpenseResponse::getAmount, months);
        return toSingleDatasetChart(byMonth, "Total Expense");
    }

    @Override
    @Transactional(readOnly = true)
    public ChartResponse getIncomeChart(Long userId, ChartGroupBy groupBy, int months) {
        List<IncomeResponse> incomes = incomeService.getIncomesByUserId(userId);

        if (groupBy == ChartGroupBy.CATEGORY) {
            YearMonth currentMonth = YearMonth.now();
            Map<String, BigDecimal> bySource = incomes.stream()
                    .filter(i -> YearMonth.from(i.getIncomeDate()).equals(currentMonth))
                    .collect(Collectors.groupingBy(i -> i.getSource().name(), LinkedHashMap::new,
                            Collectors.reducing(BigDecimal.ZERO, IncomeResponse::getAmount, BigDecimal::add)));

            return toSingleDatasetChart(bySource, "Income");
        }

        Map<String, BigDecimal> byMonth = totalsByMonth(incomes, IncomeResponse::getIncomeDate,
                IncomeResponse::getAmount, months);
        return toSingleDatasetChart(byMonth, "Total Income");
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetOverviewResponse getBudgetOverview(Long userId) {
        List<BudgetResponse> budgets = budgetService.getBudgetsByUserId(userId);

        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetResponse::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double overallPercentUsed = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetOverviewResponse.builder()
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .overallPercentUsed(round2(overallPercentUsed))
                .budgets(budgets)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ChartResponse getSavingsReport(Long userId, int months) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByUserId(userId);
        List<IncomeResponse> incomes = incomeService.getIncomesByUserId(userId);

        YearMonth currentMonth = YearMonth.now();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> incomeData = new ArrayList<>();
        List<BigDecimal> expenseData = new ArrayList<>();
        List<BigDecimal> savingsData = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            labels.add(month.toString());

            BigDecimal monthIncome = sumForMonth(incomes, IncomeResponse::getIncomeDate, IncomeResponse::getAmount, month);
            BigDecimal monthExpense = sumForMonth(expenses, ExpenseResponse::getExpenseDate, ExpenseResponse::getAmount, month);

            incomeData.add(monthIncome);
            expenseData.add(monthExpense);
            savingsData.add(monthIncome.subtract(monthExpense));
        }

        List<ChartDataset> datasets = List.of(
                ChartDataset.builder().label("Income").data(incomeData).build(),
                ChartDataset.builder().label("Expense").data(expenseData).build(),
                ChartDataset.builder().label("Savings").data(savingsData).build()
        );

        return ChartResponse.builder().labels(labels).datasets(datasets).build();
    }

    // ---------- helpers ----------

    private <T> BigDecimal sumForMonth(List<T> items, Function<T, LocalDate> dateFn,
                                       Function<T, BigDecimal> amountFn, YearMonth month) {
        return items.stream()
                .filter(item -> YearMonth.from(dateFn.apply(item)).equals(month))
                .map(amountFn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> Map<String, BigDecimal> totalsByMonth(List<T> items, Function<T, LocalDate> dateFn,
                                                      Function<T, BigDecimal> amountFn, int months) {
        YearMonth currentMonth = YearMonth.now();
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            result.put(month.toString(), sumForMonth(items, dateFn, amountFn, month));
        }

        return result;
    }

    private ChartResponse toSingleDatasetChart(Map<String, BigDecimal> data, String datasetLabel) {
        List<String> labels = new ArrayList<>(data.keySet());
        List<BigDecimal> values = new ArrayList<>(data.values());

        ChartDataset dataset = ChartDataset.builder().label(datasetLabel).data(values).build();
        return ChartResponse.builder().labels(labels).datasets(List.of(dataset)).build();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
