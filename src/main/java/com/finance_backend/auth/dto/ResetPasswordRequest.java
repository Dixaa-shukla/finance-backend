package com.finance_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class ResetPasswordRequest {

    @NotBlank(message = "token is required")
    private String token;

    /** Same policy as RegisterRequest --BCrypt note there on max = 72. */
    @NotBlank(message = "newPassword is required")
    @Size(
            min = 8,
            max = 72,
            message = "newPassword must be between 8 and 72 characters"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "newPassword must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String newPassword;
}
