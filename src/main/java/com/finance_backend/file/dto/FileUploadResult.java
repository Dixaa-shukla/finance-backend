package com.finance_backend.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Internal result of a Cloudinary upload -- used between FileStorageService and its callers. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResult {

    private String url;
    private String publicId;
}
