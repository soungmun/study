package com.example.study.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlaceSearchResponse(Meta meta, List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Meta(int totalCount, int pageableCount, boolean isEnd) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Document(
            String id,
            String placeName,
            String categoryName,
            String categoryGroupCode,
            String categoryGroupName,
            String phone,
            String addressName,
            String roadAddressName,
            String x,
            String y,
            String placeUrl,
            String distance
    ) {}
}