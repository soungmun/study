package com.example.study.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "아이디는 영문/숫자/_ 4~20자")
        String username,

        @NotBlank
        @Size(min = 6, max = 50, message = "비밀번호는 6~50자")
        String password,

        @Size(max = 50)
        String nickname,

        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 200)
        String email
) {}