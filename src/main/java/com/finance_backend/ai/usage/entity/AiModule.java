package com.finance_backend.ai.usage.entity;

/**
 * one value per AI-consuming service, so the
 * Admin Panel can break usage down by module .

 * RECEIPT_EXTRACTION covers file.service.ReceiptServiceImpl, which calls the
 * same ChatClient beans to parse OCR text even though it lives outside ai/.
 */
public enum AiModule {
    FINANCIAL_ASSISTANT,
    EXPENSE_CATEGORIZATION,
    SPENDING_ANALYTICS,
    RECEIPT_EXTRACTION
}
