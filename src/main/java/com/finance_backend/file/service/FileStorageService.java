package com.finance_backend.file.service;

import com.finance_backend.file.dto.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** Uploads to Cloudinary under the given folder (e.g. "receipts", "profile-images"). */
    FileUploadResult upload(MultipartFile file, String folder);

    /** Deletes an asset from Cloudinary by its public id. Safe to call even if the asset no longer exists. */
    void delete(String publicId);
}
