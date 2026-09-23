package com.finance_backend.ai.expenseCategorization.service;

import com.finance_backend.ai.expenseCategorization.dto.CategorizationConfirmRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationRequest;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationResponse;
import com.finance_backend.ai.expenseCategorization.dto.CategorizationSource;
import com.finance_backend.ai.expenseCategorization.entity.MerchantCategoryMapping;
import com.finance_backend.ai.expenseCategorization.repository.MerchantCategoryMappingRepository;
import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.service.AiUsageTrackingService;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.service.CategoryService;
import com.finance_backend.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CategorizationServiceImpl implements CategorizationService {

    private static final String DEFAULT_FALLBACK_CATEGORY = "Other";

    /** Identifies this service's calls in the ai_usage_log table. */
    private static final AiModule AI_MODULE = AiModule.EXPENSE_CATEGORIZATION;

    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;
    private final MerchantCategoryMappingRepository mappingRepository;
    private final CategoryService categoryService;
    private final AiUsageTrackingService aiUsageTrackingService;

    public CategorizationServiceImpl(@Qualifier("primaryChatClient") ChatClient primaryChatClient,
                                     @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
                                     MerchantCategoryMappingRepository mappingRepository,
                                     CategoryService categoryService,
                                     AiUsageTrackingService aiUsageTrackingService) {
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.mappingRepository = mappingRepository;
        this.categoryService = categoryService;
        this.aiUsageTrackingService = aiUsageTrackingService;
    }

    @Override
    @Transactional
    public CategorizationResponse suggestCategory(CategorizationRequest request) {
        String merchantKey = normalize(request.getDescription());

        Optional<MerchantCategoryMapping> learned =
                mappingRepository.findByUserIdAndMerchantKey(request.getUserId(), merchantKey);

        if (learned.isPresent()) {
            MerchantCategoryMapping mapping = learned.get();
            return CategorizationResponse.builder()
                    .detectedMerchant(mapping.getMerchantKey())
                    .suggestedCategory(mapping.getCategory())
                    .source(CategorizationSource.LEARNED)
                    .build();
        }

        List<String> allowedCategories = categoryService
                .getCategoriesForUser(request.getUserId(), CategoryType.EXPENSE)
                .stream()
                .map(CategoryResponse::getName)
                .distinct()
                .toList();

        AiCategorizationResult aiResult =
                callAiWithFallback(request.getUserId(), request.getDescription(), allowedCategories);

        String finalCategory = resolveFinalCategory(aiResult.category(), allowedCategories);

        return CategorizationResponse.builder()
                .detectedMerchant(aiResult.merchant())
                .suggestedCategory(finalCategory)
                .source(CategorizationSource.AI_SUGGESTED)
                .build();
    }

    @Override
    @Transactional
    public CategorizationResponse confirmCategorization(CategorizationConfirmRequest request) {
        String merchantKey = normalize(request.getMerchant());

        MerchantCategoryMapping mapping = mappingRepository
                .findByUserIdAndMerchantKey(request.getUserId(), merchantKey)
                .orElseGet(() -> MerchantCategoryMapping.builder()
                        .userId(request.getUserId())
                        .merchantKey(merchantKey)
                        .timesConfirmed(0)
                        .build());

        mapping.setCategory(request.getConfirmedCategory());
        mapping.setTimesConfirmed(mapping.getTimesConfirmed() + 1);

        MerchantCategoryMapping saved = mappingRepository.save(mapping);

        return CategorizationResponse.builder()
                .detectedMerchant(saved.getMerchantKey())
                .suggestedCategory(saved.getCategory())
                .source(CategorizationSource.LEARNED)
                .build();
    }

    // ---------- helpers ----------

    private String resolveFinalCategory(String aiCategory, List<String> allowedCategories) {
        if (allowedCategories.contains(aiCategory)) {
            return aiCategory;
        }
        if (allowedCategories.contains(DEFAULT_FALLBACK_CATEGORY)) {
            return DEFAULT_FALLBACK_CATEGORY;
        }
        return allowedCategories.isEmpty() ? DEFAULT_FALLBACK_CATEGORY : allowedCategories.get(0);
    }

    /**
     * userId is carried in purely so each attempt can be attributed in
     * ai.usage -- it has no effect on the prompt or the fallback decision.
     */
    private AiCategorizationResult callAiWithFallback(Long userId,
                                                      String description,
                                                      List<String> allowedCategories) {
        String categoryListText = allowedCategories.isEmpty()
                ? "Food, Transport, Shopping, Bills, Entertainment, Other"
                : String.join(", ", allowedCategories);

        String promptText = """
                You are an expense categorization engine for a personal finance app.
                Given the expense description below, identify a short, clean merchant
                name and pick the SINGLE best category from this exact list (use the
                spelling exactly as given -- do not invent new categories):
                %s

                Expense description: "%s"
                """.formatted(categoryListText, description);

        long startedAt = System.nanoTime();
        try {
            AiCategorizationResult result = primaryChatClient.prompt()
                    .user(promptText)
                    .call()
                    .entity(AiCategorizationResult.class);
            aiUsageTrackingService.recordSuccess(AI_MODULE, userId, false, elapsedMs(startedAt));
            return result;
        } catch (Exception primaryFailure) {
            aiUsageTrackingService.recordFailure(AI_MODULE, userId, false, elapsedMs(startedAt),
                    primaryFailure.getMessage());
            log.warn("Primary AI provider failed during categorization, retrying with fallback", primaryFailure);
            long fallbackStartedAt = System.nanoTime();
            try {
                AiCategorizationResult result = fallbackChatClient.prompt()
                        .user(promptText)
                        .call()
                        .entity(AiCategorizationResult.class);
                aiUsageTrackingService.recordSuccess(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt));
                return result;
            } catch (Exception fallbackFailure) {
                aiUsageTrackingService.recordFailure(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt),
                        fallbackFailure.getMessage());
                log.error("Fallback AI provider also failed during categorization", fallbackFailure);
                throw new ExternalServiceException(
                        "AI categorization is temporarily unavailable. Please pick a category manually.",
                        fallbackFailure);
            }
        }
    }

    /** nanoTime, not currentTimeMillis: only the former is monotonic. */
    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    /** Lowercase, strip punctuation, collapse whitespace -- used as the mapping table's lookup key. */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Internal shape for Spring AI's structured-output parsing -- never exposed via the controller. */
    private record AiCategorizationResult(String merchant, String category) {
    }
}
