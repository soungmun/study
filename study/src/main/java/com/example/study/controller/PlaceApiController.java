package com.example.study.controller;

import com.example.study.dto.response.PlaceSearchResponse;
import com.example.study.service.PlaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 장소 및 위치 검색 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 키워드를 통한 장소 검색, 지도 마커 정보 등을 제공하는 API를 포함합니다.
 */
@RestController
@RequestMapping("/api/places")
public class PlaceApiController {

    private final PlaceService placeService;

    public PlaceApiController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/search")
    public PlaceSearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "15") int size) {
        return placeService.search(query, page, size);
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(
            @RequestParam("query") String query) {
        return placeService.autocomplete(query);
    }

    @GetMapping("/nearby")
    public PlaceSearchResponse nearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "category", defaultValue = "FD6") String category,
            @RequestParam(value = "radius", defaultValue = "800") int radius,
            @RequestParam(value = "size", defaultValue = "15") int size) {
        return placeService.searchNearby(lat, lng, category, radius, size);
    }
}
