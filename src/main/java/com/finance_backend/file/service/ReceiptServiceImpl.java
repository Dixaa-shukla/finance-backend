package com.finance_backend.file.service;

import com.finance_backend.ai.usage.entity.AiModule;
import com.finance_backend.ai.usage.service.AiUsageTrackingService;
import com.finance_backend.category.dto.CategoryResponse;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.service.CategoryService;
import com.finance_backend.expense.dto.ExpenseRequest;
import com.finance_backend.expense.dto.ExpenseResponse;
import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.file.dto.FileUploadResult;
import com.finance_backend.file.dto.ReceiptUploadResponse;
import com.finance_backend.file.entity.StoredFile;
import com.finance_backend.file.entity.StoredFileType;
import com.finance_backend.file.repository.StoredFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Process: Upload to Cloudinary -> read text with OCR -> use AI to extract details -> create the expense.
 * If OCR or AI fails, the file upload still succeeds.
 */
@Slf4j
@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final String CLOUDINARY_FOLDER = "receipts";
    private static final String DEFAULT_FALLBACK_CATEGORY = "Other";
    private static final String DEFAULT_PAYMENT_METHOD_NOTE =
            "Auto-extracted from receipt via OCR. Please verify payment method.";

    /** Identifies this service's calls in the ai_usage_log table. */
    private static final AiModule AI_MODULE = AiModule.RECEIPT_EXTRACTION;

    private final FileStorageService fileStorageService;
    private final OcrService ocrService;
    private final StoredFileRepository storedFileRepository;
    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;
    private final AiUsageTrackingService aiUsageTrackingService;

    public ReceiptServiceImpl(FileStorageService fileStorageService,
                              OcrService ocrService,
                              StoredFileRepository storedFileRepository,
                              ExpenseService expenseService,
                              CategoryService categoryService,
                              @Qualifier("primaryChatClient") ChatClient primaryChatClient,
                              @Qualifier("fallbackChatClient") ChatClient fallbackChatClient,
                              AiUsageTrackingService aiUsageTrackingService) {
        this.fileStorageService = fileStorageService;
        this.ocrService = ocrService;
        this.storedFileRepository = storedFileRepository;
        this.expenseService = expenseService;
        this.categoryService = categoryService;
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
        this.aiUsageTrackingService = aiUsageTrackingService;
    }

    @Override
    @Transactional
    public ReceiptUploadResponse uploadAndProcessReceipt(Long userId, MultipartFile file) {
        FileUploadResult uploadResult = fileStorageService.upload(file, CLOUDINARY_FOLDER);

        String ocrText = ocrService.extractText(file);

        ReceiptExtractionResult extracted = null;
        if (StringUtils.hasText(ocrText)) {
            extracted = tryExtractWithAi(userId, ocrText);
        }

        Long createdExpenseId = null;
        boolean autoSaved = false;
        BigDecimal parsedAmount = null;
        LocalDate parsedDate = null;

        if (extracted != null) {
            parsedAmount = parseAmount(extracted.amount());
            parsedDate = parseDate(extracted.date());

            if (parsedAmount != null && StringUtils.hasText(extracted.merchant())) {
                ExpenseRequest expenseRequest = ExpenseRequest.builder()
                        .userId(userId)
                        .amount(parsedAmount)
                        .category(resolveCategory(extracted.category(), userId))
                        .merchant(extracted.merchant())
                        .expenseDate(parsedDate != null ? parsedDate : LocalDate.now())
                        .paymentMethod(PaymentMethod.OTHER)
                        .notes(DEFAULT_PAYMENT_METHOD_NOTE)
                        .receiptUrl(uploadResult.getUrl())
                        .build();

                ExpenseResponse createdExpense = expenseService.createExpense(expenseRequest);
                createdExpenseId = createdExpense.getId();
                autoSaved = true;
            }
        }

        StoredFile storedFile = StoredFile.builder()
                .userId(userId)
                .fileType(StoredFileType.RECEIPT)
                .originalFilename(file.getOriginalFilename())
                .storedUrl(uploadResult.getUrl())
                .cloudinaryPublicId(uploadResult.getPublicId())
                .fileSizeBytes(file.getSize())
                .contentType(file.getContentType())
                .relatedEntityType(autoSaved ? "EXPENSE" : null)
                .relatedEntityId(createdExpenseId)
                .build();
        StoredFile savedStoredFile = storedFileRepository.save(storedFile);

        return ReceiptUploadResponse.builder()
                .storedFileId(savedStoredFile.getId())
                .receiptUrl(uploadResult.getUrl())
                .ocrRawText(ocrText)
                .extractedMerchant(extracted != null ? extracted.merchant() : null)
                .extractedAmount(parsedAmount)
                .extractedDate(parsedDate)
                .extractedCategory(extracted != null ? extracted.category() : null)
                .autoSaved(autoSaved)
                .createdExpenseId(createdExpenseId)
                .build();
    }

    // ---------- helpers ----------

    private String resolveCategory(String aiCategory, Long userId) {
        List<String> allowed = categoryService.getCategoriesForUser(userId, CategoryType.EXPENSE)
                .stream()
                .map(CategoryResponse::getName)
                .toList();

        if (aiCategory != null && allowed.contains(aiCategory)) {
            return aiCategory;
        }
        return allowed.contains(DEFAULT_FALLBACK_CATEGORY) ? DEFAULT_FALLBACK_CATEGORY
                : (allowed.isEmpty() ? DEFAULT_FALLBACK_CATEGORY : allowed.get(0));
    }

    private ReceiptExtractionResult tryExtractWithAi(Long userId, String ocrText) {
        List<String> allowedCategories = categoryService.getCategoriesForUser(userId, CategoryType.EXPENSE)
                .stream()
                .map(CategoryResponse::getName)
                .toList();
        String categoryListText = allowedCategories.isEmpty()
                ? "Food, Transport, Shopping, Bills, Entertainment, Other"
                : String.join(", ", allowedCategories);

        String promptText = """
                You are a receipt-parsing engine for a personal finance app. The text
                below was extracted via OCR from a receipt photo and may contain OCR
                errors/noise -- do your best to interpret it. Extract:
                - merchant: the store/business name
                - amount: the FINAL total amount paid (as a plain number, no currency symbol)
                - date: the transaction date in YYYY-MM-DD format if found, else leave blank
                - gstAmount: the GST/tax amount as a plain number if shown, else leave blank
                - category: the single best fit from this exact list (use the spelling
                  exactly as given): %s

                If you cannot confidently determine the merchant AND amount, return
                empty strings for those two fields -- do not guess.

                OCR text:
                %s
                """.formatted(categoryListText, ocrText);

        long startedAt = System.nanoTime();
        try {
            ReceiptExtractionResult result =
                    primaryChatClient.prompt().user(promptText).call().entity(ReceiptExtractionResult.class);
            aiUsageTrackingService.recordSuccess(AI_MODULE, userId, false, elapsedMs(startedAt));
            return result;
        } catch (Exception primaryFailure) {
            aiUsageTrackingService.recordFailure(AI_MODULE, userId, false, elapsedMs(startedAt),
                    primaryFailure.getMessage());
            log.warn("Primary AI provider failed during receipt extraction, retrying with fallback", primaryFailure);
            long fallbackStartedAt = System.nanoTime();
            try {
                ReceiptExtractionResult result =
                        fallbackChatClient.prompt().user(promptText).call().entity(ReceiptExtractionResult.class);
                aiUsageTrackingService.recordSuccess(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt));
                return result;
            } catch (Exception fallbackFailure) {
                aiUsageTrackingService.recordFailure(AI_MODULE, userId, true, elapsedMs(fallbackStartedAt),
                        fallbackFailure.getMessage());
                // Extraction is enrichment, not the core value -- the upload already
                // succeeded. Log and let the caller fall back to manual entry.
                log.warn("Fallback AI provider also failed during receipt extraction -- "
                        + "returning raw OCR text only for userId={}", userId, fallbackFailure);
                return null;
            }
        }
    }

    /** nanoTime, not currentTimeMillis: only the former is monotonic. */
    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private BigDecimal parseAmount(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            String cleaned = raw.replaceAll("[^0-9.]", "");
            if (cleaned.isBlank()) {
                return null;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Internal shape for Spring AI's structured-output parsing -- never exposed via the controller. */
    private record ReceiptExtractionResult(String merchant, String amount, String date,
                                           String gstAmount, String category) {
    }
}
