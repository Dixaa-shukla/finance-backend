package com.finance_backend.ai.expenseCategorization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationResponse {

    private String detectedMerchant;
    private String suggestedCategory;
    private CategorizationSource source;
}
