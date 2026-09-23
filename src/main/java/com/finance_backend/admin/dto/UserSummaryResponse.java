package com.finance_backend.admin.dto;

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
public class UserSummaryResponse {

    /** The authenticated user ID every module references (Module 1). */
    private Long userId;
    private String fullName;
    private String preferredCurrency;
    private String profilePictureUrl;
    private long expenseCount;
    private long incomeCount;
    /** When the profile was first created. */
    private LocalDateTime joinedAt;
}
