package com.finance_backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptUploadResponse {

    private Long storedFileId;
    private String receiptUrl;

    /** Always returns original OCR text so user can read it, even if automatic expense creation fails. */
    private String ocrRawText;

    private String extractedMerchant;
    private BigDecimal extractedAmount;
    private LocalDate extractedDate;
    private String extractedCategory;

    /** True means receipt automatically created an expense; false means upload succeeded but manual entry is needed. */
    private boolean autoSaved;
    /** Populated only when autoSaved is true. */
    private Long createdExpenseId;
}
