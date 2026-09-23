package com.finance_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /** The userId every other module refers to. */
    private Long id;

    private String email;

    /** Without the ROLE_ prefix, e.g. ["USER"]. */
    private Set<String> roles;

    private boolean enabled;

    /** "LOCAL" for email/password accounts, "GOOGLE" for Google sign-in. */
    private String provider;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
