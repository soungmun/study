package com.example.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank String token,

        @NotBlank
        @Size(min = 6, max = 50, message = "비밀번호는 6~50자")
        String newPassword
) {}