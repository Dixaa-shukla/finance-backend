package com.finance_backend.file.controller;

import com.finance_backend.file.dto.ProfileImageUploadResponse;
import com.finance_backend.file.dto.ReceiptUploadResponse;
import com.finance_backend.file.dto.StoredFileResponse;
import com.finance_backend.file.entity.StoredFile;
import com.finance_backend.file.exception.StoredFileNotFoundException;
import com.finance_backend.file.repository.StoredFileRepository;
import com.finance_backend.file.service.FileStorageService;
import com.finance_backend.file.service.ProfileImageService;
import com.finance_backend.file.service.ReceiptService;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final ReceiptService receiptService;
    private final ProfileImageService profileImageService;
    private final StoredFileRepository storedFileRepository;
    private final FileStorageService fileStorageService;
    private final ResourceOwnershipGuard ownershipGuard;

    public FileController(ReceiptService receiptService,
                          ProfileImageService profileImageService,
                          StoredFileRepository storedFileRepository,
                          FileStorageService fileStorageService,
                          ResourceOwnershipGuard ownershipGuard) {
        this.receiptService = receiptService;
        this.profileImageService = profileImageService;
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.ownershipGuard = ownershipGuard;
    }

    /**
     * Uploads a receipt, reads it with OCR and AI, and creates an expense automatically when confident.
     * Otherwise, it returns the OCR text so the user can enter the details manually.
     */
    @PostMapping(value = "/receipt/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ReceiptUploadResponse> uploadReceipt(@PathVariable Long userId,
                                                               @RequestParam("file") MultipartFile file) {
        ReceiptUploadResponse response = receiptService.uploadAndProcessReceipt(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Upload a profile image. Replaces any previous one.
     * POST /api/v1/files/profile-image/{userId}  (multipart/form-data, field name "file")
     */
    @PostMapping(value = "/profile-image/{userId}", consumes = "multipart/form-data")
    public ResponseEntity<ProfileImageUploadResponse> uploadProfileImage(@PathVariable Long userId,
                                                                         @RequestParam("file") MultipartFile file) {
        ProfileImageUploadResponse response = profileImageService.uploadProfileImage(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List every file a user has uploaded (receipts + profile images).
     * GET /api/v1/files/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<StoredFileResponse>> getFilesForUser(@PathVariable Long userId) {
        List<StoredFileResponse> files = storedFileRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(files);
    }

    /**
     * Delete a stored file -- removes it from Cloudinary and from the database.
     * Does NOT delete an auto-created Expense that resulted from a receipt.
     * DELETE /api/v1/files/{id}
     *
     * ⚠️ THE OWNERSHIP CHECK IS IN THE CONTROLLER HERE, NOT IN A SERVICE, BECAUSE
     * THIS ROUTE HAS NO SERVICE. It talks to StoredFileRepository directly, unlike
     * the upload routes above. FileStorageServiceImpl is a pure Cloudinary wrapper
     * that never sees a userId, so it is not the place for it.
     *
     * The check must run BEFORE the Cloudinary delete: that call is irreversible,
     * so a 403 raised afterwards would still have destroyed the image.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        StoredFile file = storedFileRepository.findById(id)
                .orElseThrow(() -> new StoredFileNotFoundException(id));
        ownershipGuard.check(file.getUserId(), "stored file", id);
        fileStorageService.delete(file.getCloudinaryPublicId());
        storedFileRepository.delete(file);
        return ResponseEntity.noContent().build();
    }

    private StoredFileResponse toResponse(StoredFile file) {
        return StoredFileResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType())
                .originalFilename(file.getOriginalFilename())
                .storedUrl(file.getStoredUrl())
                .fileSizeBytes(file.getFileSizeBytes())
                .contentType(file.getContentType())
                .relatedEntityType(file.getRelatedEntityType())
                .relatedEntityId(file.getRelatedEntityId())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}
