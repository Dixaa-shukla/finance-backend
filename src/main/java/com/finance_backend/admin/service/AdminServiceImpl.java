package com.finance_backend.admin.service;

import com.finance_backend.admin.dto.PlatformAnalyticsResponse;
import com.finance_backend.admin.dto.PlatformOverviewResponse;
import com.finance_backend.admin.dto.PlatformReportResponse;
import com.finance_backend.admin.dto.UserDetailResponse;
import com.finance_backend.admin.dto.UserSummaryResponse;
import com.finance_backend.ai.expenseCategorization.repository.MerchantCategoryMappingRepository;
import com.finance_backend.ai.financialAssistant.repository.ChatHistoryRepository;
import com.finance_backend.ai.spendingAnalytics.dto.HealthScoreResponse;
import com.finance_backend.ai.spendingAnalytics.repository.MonthlyReportRepository;
import com.finance_backend.ai.spendingAnalytics.service.AnalyticsService;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.repository.ExpenseRepository;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.repository.IncomeRepository;
import com.finance_backend.income.service.IncomeService;
import com.finance_backend.investmentTracker.repository.InvestmentRepository;
import com.finance_backend.profile.dto.ProfileResponse;
import com.finance_backend.profile.entity.UserProfile;
import com.finance_backend.profile.repository.UserProfileRepository;
import com.finance_backend.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    // Module services -- reused so admin never re-implements module logic.
    private final ProfileService profileService;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final AnalyticsService analyticsService;
    private final AiUsageMonitoringService aiUsageMonitoringService;

    // Repositories -- only for aggregates no service exposes (counts/sums).
    private final UserProfileRepository userProfileRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final InvestmentRepository investmentRepository;
    // The three AI repositories stay wired here for getUserDetail's PER-USER
    // counts. Platform-wide AI totals belong to AiUsageMonitoringService.
    private final ChatHistoryRepository chatHistoryRepository;
    private final MerchantCategoryMappingRepository merchantCategoryMappingRepository;
    private final MonthlyReportRepository monthlyReportRepository;

    public AdminServiceImpl(ProfileService profileService,
                            ExpenseService expenseService,
                            IncomeService incomeService,
                            AnalyticsService analyticsService,
                            AiUsageMonitoringService aiUsageMonitoringService,
                            UserProfileRepository userProfileRepository,
                            ExpenseRepository expenseRepository,
                            IncomeRepository incomeRepository,
                            CategoryRepository categoryRepository,
                            InvestmentRepository investmentRepository,
                            ChatHistoryRepository chatHistoryRepository,
                            MerchantCategoryMappingRepository merchantCategoryMappingRepository,
                            MonthlyReportRepository monthlyReportRepository) {
        this.profileService = profileService;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.analyticsService = analyticsService;
        this.aiUsageMonitoringService = aiUsageMonitoringService;
        this.userProfileRepository = userProfileRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.categoryRepository = categoryRepository;
        this.investmentRepository = investmentRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.merchantCategoryMappingRepository = merchantCategoryMappingRepository;
        this.monthlyReportRepository = monthlyReportRepository;
    }

    // ==================== USER MANAGEMENT ====================

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(Pageable pageable) {
        // Paginate at the DB level, then enrich each row with cheap COUNT
        // queries. Deliberately not loading each user's expense/income rows
        return userProfileRepository.findAll(pageable).map(this::toUserSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        // Reused so a missing user throws ProfileNotFoundException -> 404 via
        // GlobalExceptionHandler, with no duplicated lookup logic.
        ProfileResponse profile = profileService.getProfileByUserId(userId);

        List<ExpenseResponse> expenses = expenseService.getExpensesByUserId(userId);
        List<IncomeResponse> incomes = incomeService.getIncomesByUserId(userId);

        BigDecimal totalSpent = sum(expenses, ExpenseResponse::getAmount);
        BigDecimal totalEarned = sum(incomes, IncomeResponse::getAmount);

        HealthScoreResponse healthScore = safeHealthScore(userId);

        return UserDetailResponse.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .preferredCurrency(profile.getPreferredCurrency())
                .primaryFinancialGoal(profile.getPrimaryFinancialGoal())
                .monthlySalary(profile.getMonthlySalary())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .expenseCount(expenses.size())
                .incomeCount(incomes.size())
                .totalSpent(totalSpent)
                .totalEarned(totalEarned)
                .netSavings(totalEarned.subtract(totalSpent))
                .aiChatMessageCount(chatHistoryRepository.countByUserId(userId))
                .learnedMerchantCount(merchantCategoryMappingRepository.countByUserId(userId))
                .monthlyReportCount(monthlyReportRepository.countByUserId(userId))
                .healthScore(healthScore != null ? healthScore.getScore() : 0)
                .healthScoreLabel(healthScore != null ? healthScore.getLabel() : "Unavailable")
                .joinedAt(profile.getCreatedAt())
                .lastUpdatedAt(profile.getUpdatedAt())
                .build();
    }

    // ==================== ANALYTICS ====================

    @Override
    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getPlatformAnalytics() {
        long totalCategories = categoryRepository.count();
        long defaultCategories = categoryRepository.countByUserIdIsNull();

        BigDecimal totalSpent = expenseRepository.sumAllAmounts();
        BigDecimal totalEarned = incomeRepository.sumAllAmounts();

        PlatformOverviewResponse overview = PlatformOverviewResponse.builder()
                .totalUsers(userProfileRepository.count())
                .totalExpenses(expenseRepository.count())
                .totalIncomes(incomeRepository.count())
                .totalInvestments(investmentRepository.count())
                .totalCategories(totalCategories)
                .defaultCategories(defaultCategories)
                .customCategories(totalCategories - defaultCategories)
                .build();

        return PlatformAnalyticsResponse.builder()
                .overview(overview)
                .platformTotalSpent(totalSpent)
                .platformTotalEarned(totalEarned)
                .platformNetFlow(totalEarned.subtract(totalSpent))
                .build();
    }

    // ==================== REPORTS ====================

    @Override
    @Transactional(readOnly = true)
    public PlatformReportResponse getPlatformReport() {
        return PlatformReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .analytics(getPlatformAnalytics())
                .aiUsage(aiUsageMonitoringService.getAiUsage())
                .build();
    }

    // ---------- helpers ----------

    private UserSummaryResponse toUserSummary(UserProfile profile) {
        Long userId = profile.getUserId();

        return UserSummaryResponse.builder()
                .userId(userId)
                .fullName(profile.getFullName())
                .preferredCurrency(profile.getPreferredCurrency())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .expenseCount(expenseRepository.countByUserId(userId))
                .incomeCount(incomeRepository.countByUserId(userId))
                .joinedAt(profile.getCreatedAt())
                .build();
    }

    /**
     * The health score is computed by the analytics module, which can reach an
     * external AI provider.
     */
    private HealthScoreResponse safeHealthScore(Long userId) {
        try {
            return analyticsService.getHealthScore(userId);
        } catch (Exception ex) {
            log.warn("Health score unavailable for userId={} in admin view: {}", userId, ex.getMessage());
            return null;
        }
    }

    private <T> BigDecimal sum(List<T> items, java.util.function.Function<T, BigDecimal> amountFn) {
        return items.stream()
                .map(amountFn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
