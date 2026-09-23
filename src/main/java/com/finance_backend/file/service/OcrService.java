package com.finance_backend.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * Uses Tesseract to read text from an image.
     * If OCR fails, it returns empty text so the user can enter the details manually.
     */
    String extractText(MultipartFile file);
}
