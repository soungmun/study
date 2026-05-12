package com.example.study.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KakaoPayReadyRequest(
        @NotBlank String itemName,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Min(1) Integer totalAmount
) {}
