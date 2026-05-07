package com.example.study.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BookSearchResponse(Meta meta, List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Meta(
            int totalCount,
            int pageableCount,
            boolean isEnd
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Document(
            String title,
            String contents,
            String url,
            String isbn,
            String datetime,
            List<String> authors,
            String publisher,
            List<String> translators,
            int price,
            int salePrice,
            String thumbnail,
            String status
    ) {}
}