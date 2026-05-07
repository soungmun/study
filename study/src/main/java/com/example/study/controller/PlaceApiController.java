package com.example.study.controller;

import com.example.study.dto.response.PlaceSearchResponse;
import com.example.study.service.PlaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceApiController {

    private final PlaceService placeService;

    public PlaceApiController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/search")
    public PlaceSearchResponse search(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return placeService.search(query, page, size);
    }
}