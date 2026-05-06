package com.example.study.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceSearchResponse(Meta meta, List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(int total_count, int pageable_count, boolean is_end) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            String id,
            String place_name,
            String category_name,
            String category_group_code,
            String category_group_name,
            String phone,
            String address_name,
            String road_address_name,
            String x,
            String y,
            String place_url,
            String distance
    ) {}
}