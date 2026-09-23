package com.finance_backend.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;

    private Long userId;

    private String fullName;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String gender;

    private BigDecimal monthlySalary;

    private String preferredCurrency;

    private String primaryFinancialGoal;

    private String profilePictureUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}