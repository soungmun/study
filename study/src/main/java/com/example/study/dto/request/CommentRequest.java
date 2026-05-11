package com.example.study.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "내용을 입력하세요.")
        @Size(max = 2000, message = "댓글은 2000자 이하로 입력해 주세요.")
        String content
) {}