package com.finance_backend.file.dto;

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
public class ProfileImageUploadResponse {

    private Long storedFileId;
    private String profileImageUrl;
    private LocalDateTime uploadedAt;
}
