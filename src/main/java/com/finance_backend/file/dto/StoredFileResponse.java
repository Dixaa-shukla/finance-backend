package com.finance_backend.file.dto;

import com.finance_backend.file.entity.StoredFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFileResponse {

    private Long id;
    private StoredFileType fileType;
    private String originalFilename;
    private String storedUrl;
    private Long fileSizeBytes;
    private String contentType;
    private String relatedEntityType;
    private Long relatedEntityId;
    private LocalDateTime uploadedAt;
}
