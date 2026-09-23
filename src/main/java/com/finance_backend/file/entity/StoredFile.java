package com.finance_backend.file.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Module 17: Stores information about files uploaded to Cloudinary.
 * Keeps the Cloudinary file ID so uploaded files can be deleted later when needed.
 */
@Entity
@Table(name = "stored_files", indexes = {
        @Index(name = "idx_stored_files_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private StoredFileType fileType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "stored_url", nullable = false, length = 500)
    private String storedUrl;

    /** Cloudinary's asset identifier -- required to delete the asset later. */
    @Column(name = "cloudinary_public_id", nullable = false, length = 255)
    private String cloudinaryPublicId;

    private Long fileSizeBytes;

    @Column(length = 100)
    private String contentType;

    /** e.g. "EXPENSE" if this receipt auto-created an expense. Null for profile images. */
    @Column(name = "related_entity_type", length = 30)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
