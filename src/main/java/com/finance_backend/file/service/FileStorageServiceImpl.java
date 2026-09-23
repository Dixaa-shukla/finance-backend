package com.finance_backend.file.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.common.exception.ExternalServiceException;
import com.finance_backend.file.dto.FileUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    public FileStorageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public FileUploadResult upload(MultipartFile file, String folder) {

        validateImage(file);

        try {
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            if (secureUrl == null || publicId == null) {
                log.error("Cloudinary returned incomplete upload response");
                throw new ExternalServiceException(
                        "File upload failed. Invalid response from Cloudinary."
                );
            }

            log.info(
                    "File uploaded successfully to Cloudinary. folder={}, publicId={}",
                    folder,
                    publicId
            );

            return FileUploadResult.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .build();

        } catch (ExternalServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);

            throw new ExternalServiceException(
                    "File upload failed. Please try again.",
                    e
            );
        }
    }

    @Override
    public void delete(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            log.info(
                    "Cloudinary asset deleted. publicId={}, result={}",
                    publicId,
                    result.get("result")
            );

        } catch (Exception e) {

            /*
             * Deleting an old profile image should not prevent
             * the user from updating their profile.
             */
            log.warn(
                    "Failed to delete Cloudinary asset {}. Continuing.",
                    publicId,
                    e
            );
        }
    }

    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Profile image must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    "Profile image must not exceed 5 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {

            throw new BadRequestException(
                    "Only JPEG, PNG, and WebP images are allowed"
            );
        }
    }
}
