package com.finance_backend.file.service;

import com.finance_backend.file.dto.ProfileImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageService {

    ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile file);
}
