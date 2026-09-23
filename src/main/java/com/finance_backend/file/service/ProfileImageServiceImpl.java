package com.finance_backend.file.service;

import com.finance_backend.file.dto.FileUploadResult;
import com.finance_backend.file.dto.ProfileImageUploadResponse;
import com.finance_backend.file.entity.StoredFile;
import com.finance_backend.file.entity.StoredFileType;
import com.finance_backend.file.repository.StoredFileRepository;
import com.finance_backend.profile.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ProfileImageServiceImpl implements ProfileImageService {

    private static final String CLOUDINARY_FOLDER = "profile-images";

    private final FileStorageService fileStorageService;
    private final StoredFileRepository storedFileRepository;
    private final ProfileService profileService;

    public ProfileImageServiceImpl(FileStorageService fileStorageService,
                                   StoredFileRepository storedFileRepository,
                                   ProfileService profileService) {
        this.fileStorageService = fileStorageService;
        this.storedFileRepository = storedFileRepository;
        this.profileService = profileService;
    }

    @Override
    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile file) {
        // Keep the previous image until its replacement has been uploaded and
        // recorded successfully, so an upload failure never removes it.
        Optional<StoredFile> previous = storedFileRepository
                .findFirstByUserIdAndFileTypeOrderByUploadedAtDesc(userId, StoredFileType.PROFILE_IMAGE);

        FileUploadResult uploadResult = fileStorageService.upload(file, CLOUDINARY_FOLDER);

        // Reuses Module 2's ProfileService -- see the new updateProfilePictureByUserId method.
        profileService.updateProfilePictureByUserId(userId, uploadResult.getUrl());

        StoredFile storedFile = StoredFile.builder()
                .userId(userId)
                .fileType(StoredFileType.PROFILE_IMAGE)
                .originalFilename(file.getOriginalFilename())
                .storedUrl(uploadResult.getUrl())
                .cloudinaryPublicId(uploadResult.getPublicId())
                .fileSizeBytes(file.getSize())
                .contentType(file.getContentType())
                .build();
        StoredFile saved = storedFileRepository.save(storedFile);

        // The replacement is now safely stored and referenced by the profile.
        previous.ifPresent(old -> {
            fileStorageService.delete(old.getCloudinaryPublicId());
            storedFileRepository.delete(old);
        });

        return ProfileImageUploadResponse.builder()
                .storedFileId(saved.getId())
                .profileImageUrl(uploadResult.getUrl())
                .uploadedAt(saved.getUploadedAt())
                .build();
    }
}
