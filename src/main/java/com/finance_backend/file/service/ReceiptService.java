package com.finance_backend.file.service;

import com.finance_backend.file.dto.ReceiptUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReceiptService {

    /**
     * Uploads a receipt, uses OCR and AI to read it, and creates an expense automatically when confident.
     * The upload still works even if OCR or AI fails.
     */
    ReceiptUploadResponse uploadAndProcessReceipt(Long userId, MultipartFile file);
}
