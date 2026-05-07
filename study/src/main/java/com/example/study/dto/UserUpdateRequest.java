package com.example.study.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 50, message = "닉네임은 50자 이하")
        String nickname,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 200)
        String email,

        @Size(min = 6, max = 50, message = "현재 비밀번호는 6~50자")
        String currentPassword,

        @Size(min = 6, max = 50, message = "새 비밀번호는 6~50자")
        String newPassword
) {}