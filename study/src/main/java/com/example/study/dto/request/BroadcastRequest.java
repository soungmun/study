package com.example.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotBlank(message = "제목을 입력하세요.")
        @Size(max = 200, message = "제목은 200자 이하")
        String subject,

        @NotBlank(message = "본문을 입력하세요.")
        @Size(max = 10000, message = "본문은 10000자 이하")
        String body
) {}