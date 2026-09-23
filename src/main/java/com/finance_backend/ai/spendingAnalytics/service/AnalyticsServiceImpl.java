package com.finance_backend.ai.spendingAnalytics.service;

import com.finance_backend.ai.spendingAnalytics.dto.*;
import com.finance_backend.ai.spendingAnalytics.entity.MonthlyReport;
import com.finance_backend.ai.spendingAnalytics.exception.MonthlyReportNotFoundException;
import com.finance_backend.ai.spendingAnalytics.repository.MonthlyReportRepository;
import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.service.AiUsageTrackingService;
import com.finance_backend.budget.dto.BudgetResponse;
import com.finance_backend.budget.service.BudgetService;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.goal.dto.GoalResponse;
import com.finance_backend.goal.entity.GoalStatus;
import com.finance_backend.goal.service.GoalService;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.service.IncomeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final double OVERSPENDING_THRESHOLD_PERCENT = 20.0;
    private static final int OVERSPENDING_LOOKBACK_MONTHS = 3;
    private static final String FALLBACK_INSIGHT =
            "AI insight generation is temporarily unavailable this month. Your numeric summary above is accurate and unaffected.";

    /** Identifies this service's calls in the ai_usage_log table. */
    private static final AiModule AI_MODULE = AiModule.SPENDING_ANALYTICS;

    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final MonthlyReportRepository monthlyReportRepository;
    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;
    private final AiUsageTrackingService aiUsageTrackingService;

    public AnalyticsServiceImpl(ExpenseService expenseService,
                                IncomeService incomeService,
                                BudgetService budgetService,
                                GoalService goalService,
                                MonthlyReportRepository monthlyReportRepository,
                                @Qualifier("primaryChatClient") ChatClient primaryChatClient,
                                @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
                                AiUsageTrackingService aiUsageTrackingService) {
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.monthlyReportRepository = monthlyReportRepository;
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.aiUsageTrackingService = aiUsageTrackingService;
    }

    @Override
    @Transactional(readOnly = true)
    public HealthScoreResponse getHealthScore(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal income = totalIncomeForMonth(userId, currentMonth);
        BigDecimal expense = totalExpenseForMonth(userId, currentMonth);

        double savingsRatePercent = calculateSavingsRatePercent(income, expense);
        double savingsScore = clamp(savingsRatePercent, 0, 100) / 100.0 * 40.0;

        List<BudgetResponse> budgets = budgetService.getBudgetsByUserId(userId);
        double budgetAdherencePercent;
        double budgetScore;
        if (budgets.isEmpty()) {
            budgetAdherencePercent = 0;
            budgetScore = 15.0; // neutral credit -- not punished for having no budgets set
        } else {
            long onTrack = budgets.stream().filter(b -> !b.isAlertTriggered()).count();
            budgetAdherencePercent = (onTrack * 100.0) / budgets.size();
            budgetScore = budgetAdherencePercent / 100.0 * 30.0;
        }

        List<GoalResponse> goals = goalService.getGoalsByUserId(userId);
        double goalOnTrackPercent;
        double goalScore;
        if (goals.isEmpty()) {
            goalOnTrackPercent = 0;
            goalScore = 15.0; // neutral credit -- not punished for having no goals set
        } else {
            long onTrack = goals.stream().filter(g -> g.getStatus() != GoalStatus.EXPIRED).count();
            goalOnTrackPercent = (onTrack * 100.0) / goals.size();
            goalScore = goalOnTrackPercent / 100.0 * 30.0;
        }

        int totalScore = (int) Math.round(savingsScore + budgetScore + goalScore);
        totalScore = (int) clamp(totalScore, 0, 100);

        return HealthScoreResponse.builder()
                .userId(userId)
                .score(totalScore)
                .label(scoreLabel(totalScore))
                .savingsRatePercent(round2(savingsRatePercent))
                .savingsScoreOutOf40(round2(savingsScore))
                .budgetAdherencePercent(round2(budgetAdherencePercent))
                .budgetScoreOutOf30(round2(budgetScore))
                .goalOnTrackPercent(round2(goalOnTrackPercent))
                .goalScoreOutOf30(round2(goalScore))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpendingTrendResponse getSpendingTrends(Long userId, int months) {
        List<ExpenseResponse> allExpenses = expenseService.getExpensesByUserId(userId);
        YearMonth currentMonth = YearMonth.now();

        List<MonthlySpendingBreakdown> breakdowns = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            List<ExpenseResponse> monthExpenses = allExpenses.stream()
                    .filter(e -> YearMonth.from(e.getExpenseDate()).equals(month))
                    .toList();

            BigDecimal total = monthExpenses.stream()
                    .map(ExpenseResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> byCategory = monthExpenses.stream()
                    .collect(Collectors.groupingBy(ExpenseResponse::getCategory,
                            Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

            breakdowns.add(MonthlySpendingBreakdown.builder()
                    .month(month.toString())
                    .totalAmount(total)
                    .byCategory(byCategory)
                    .build());
        }

        return SpendingTrendResponse.builder()
                .userId(userId)
                .months(breakdowns)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverspendingAlertResponse> getOverspendingAlerts(Long userId) {
        List<ExpenseResponse> allExpenses = expenseService.getExpensesByUserId(userId);
        YearMonth currentMonth = YearMonth.now();

        Map<String, BigDecimal> currentMonthByCategory = allExpenses.stream()
                .filter(e -> YearMonth.from(e.getExpenseDate()).equals(currentMonth))
                .collect(Collectors.groupingBy(ExpenseResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

        List<YearMonth> lookbackMonths = new ArrayList<>();
        for (int i = 1; i <= OVERSPENDING_LOOKBACK_MONTHS; i++) {
            lookbackMonths.add(currentMonth.minusMonths(i));
        }

        Map<String, BigDecimal> historicalTotalsByCategory = allExpenses.stream()
                .filter(e -> lookbackMonths.contains(YearMonth.from(e.getExpenseDate())))
                .collect(Collectors.groupingBy(ExpenseResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, ExpenseResponse::getAmount, BigDecimal::add)));

        List<OverspendingAlertResponse> alerts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : currentMonthByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal currentAmount = entry.getValue();
            BigDecimal historicalTotal = historicalTotalsByCategory.getOrDefault(category, BigDecimal.ZERO);
            BigDecimal historicalAverage = historicalTotal.divide(
                    BigDecimal.valueOf(OVERSPENDING_LOOKBACK_MONTHS), 2, RoundingMode.HALF_UP);

            if (historicalAverage.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // no history to compare against -- can't flag as "over average"
            }

            double percentAbove = currentAmount.subtract(historicalAverage)
                    .divide(historicalAverage, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            if (percentAbove >= OVERSPENDING_THRESHOLD_PERCENT) {
                alerts.add(OverspendingAlertResponse.builder()
                        .category(category)
                        .currentMonthAmount(currentAmount)
                        .historicalAverageAmount(historicalAverage)
                        .percentAboveAverage(round2(percentAbove))
                        .build());
            }
        }

        return alerts;
    }

    @Override
    @Transactional
    public MonthlyReportResponse generateMonthlyReport(Long userId, YearMonth targetMonth) {
        BigDecimal income = totalIncomeForMonth(userId, targetMonth);
        BigDecimal expense = totalExpenseForMonth(userId, targetMonth);
        BigDecimal savings = income.subtract(expense);
        double savingsRatePercent = calculateSavingsRatePercent(income, expense);

        // Health score here uses the SAME formula as getHealthScore, but for
        // the target month specifically rather than "right now"(for avoid over-abstracting)
        // two call sites that differ only in which month they look at.
        HealthScoreResponse liveScore = getHealthScore(userId);

        String aiInsight = generateAiInsight(userId, targetMonth, income, expense, savings, savingsRatePercent, liveScore.getScore());

        LocalDate reportMonthDate = targetMonth.atDay(1);
        MonthlyReport report = monthlyReportRepository.findByUserIdAndReportMonth(userId, reportMonthDate)
                .orElseGet(() -> MonthlyReport.builder().userId(userId).reportMonth(reportMonthDate).build());

        report.setTotalIncome(income);
        report.setTotalExpense(expense);
        report.setSavingsRate(savingsRatePercent);
        report.setHealthScore(liveScore.getScore());
        report.setAiInsight(aiInsight);

        MonthlyReport saved = monthlyReportRepository.save(report);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Long userId, YearMonth targetMonth) {
        LocalDate reportMonthDate = targetMonth.atDay(1);
        MonthlyReport report = monthlyReportRepository.findByUserIdAndReportMonth(userId, reportMonthDate)
                .orElseThrow(() -> new MonthlyReportNotFoundException(userId, reportMonthDate));
        return toResponse(report);
    }

    // ---------- helpers ----------

    private String generateAiInsight(Long userId, YearMonth month, BigDecimal income, BigDecimal expense,
                                     BigDecimal savings, double savingsRatePercent, int healthScore) {
        String promptText = """
                You are a personal finance analyst. Write a short (3-4 sentence),
                encouraging but honest monthly summary for the user based ONLY on
                the numbers below -- do not invent any figures not given here.

                Month: %s
                Total income: Rs.%s
                Total expenses: Rs.%s
                Net savings: Rs.%s (%.1f%% savings rate)
                Financial health score: %d/100
                """.formatted(month, income, expense, savings, savingsRatePercent, healthScore);

        long startedAt = System.nanoTime();
        try {
            String insight = primaryChatClient.prompt().user(promptText).call().content();
            aiUsageTrackingService.recordSuccess(AI_MODULE, userId, false, elapsedMs(startedAt));
            return insight;
        } catch (Exception primaryFailure) {
            aiUsageTrackingService.recordFailure(AI_MODULE, userId, false, elapsedMs(startedAt),
                    primaryFailure.getMessage());
            log.warn("Primary AI provider failed generating monthly insight for userId={}, retrying with fallback",
                    userId, primaryFailure);
            long fallbackStartedAt = System.nanoTime();
            try {
                String insight = fallbackChatClient.prompt().user(promptText).call().content();
                aiUsageTrackingService.recordSuccess(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt));
                return insight;
            } catch (Exception fallbackFailure) {
                aiUsageTrackingService.recordFailure(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt),
                        fallbackFailure.getMessage());
                log.error("Fallback AI provider also failed generating monthly insight for userId={} -- "
                        + "saving report with static fallback text instead of failing the whole report", userId, fallbackFailure);
                return FALLBACK_INSIGHT;
            }
        }
    }

    /** nanoTime, not currentTimeMillis: only the former is monotonic. */
    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private BigDecimal totalIncomeForMonth(Long userId, YearMonth month) {
        return incomeService.getIncomesByUserId(userId).stream()
                .filter(i -> YearMonth.from(i.getIncomeDate()).equals(month))
                .map(IncomeResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalExpenseForMonth(Long userId, YearMonth month) {
        return expenseService.getExpensesByUserId(userId).stream()
                .filter(e -> YearMonth.from(e.getExpenseDate()).equals(month))
                .map(ExpenseResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculateSavingsRatePercent(BigDecimal income, BigDecimal expense) {
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return income.subtract(expense)
                .divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String scoreLabel(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        return "Needs Attention";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private MonthlyReportResponse toResponse(MonthlyReport report) {
        return MonthlyReportResponse.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .month(YearMonth.from(report.getReportMonth()).format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .totalIncome(report.getTotalIncome())
                .totalExpense(report.getTotalExpense())
                .savingsAmount(report.getTotalIncome().subtract(report.getTotalExpense()))
                .savingsRatePercent(round2(report.getSavingsRate()))
                .healthScore(report.getHealthScore())
                .healthScoreLabel(scoreLabel(report.getHealthScore()))
                .aiInsight(report.getAiInsight())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}
