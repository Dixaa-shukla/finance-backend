package com.finance_backend.file.repository;

import com.finance_backend.file.entity.StoredFile;
import com.finance_backend.file.entity.StoredFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findByUserIdOrderByUploadedAtDesc(Long userId);

    /** Used to find (and delete) a user's previous profile image before saving a new one. */
    Optional<StoredFile> findFirstByUserIdAndFileTypeOrderByUploadedAtDesc(Long userId, StoredFileType fileType);
}