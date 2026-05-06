package com.example.study.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookSearchResponse(Meta meta, List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            int total_count,
            int pageable_count,
            boolean is_end
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
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
            int sale_price,
            String thumbnail,
            String status
    ) {}
}