package com.example.study.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordForgotRequest(
        @NotBlank
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {}