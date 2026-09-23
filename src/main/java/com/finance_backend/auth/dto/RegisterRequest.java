package com.finance_backend.auth.dto;

import jakarta.validation.constraints.Email;
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
public class RegisterRequest {

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(
            max = 150,
            message = "email must not exceed 150 characters"
    )
    private String email;

    @NotBlank(message = "password is required")
    @Size(
            min = 8,
            max = 72,
            message = "password must be between 8 and 72 characters"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String password;
}
