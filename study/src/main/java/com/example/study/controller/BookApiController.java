package com.example.study.controller;

import com.example.study.dto.response.BookSearchResponse;
import com.example.study.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도서 정보 검색 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 책 검색 및 상세 정보 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/books")
public class BookApiController {

    private final BookService bookService;

    public BookApiController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/search")
    public BookSearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(value = "sort", required = false, defaultValue = "accuracy") String sort,
            @RequestParam(value = "target", required = false) String target,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return bookService.search(query, sort, target, page, size);
    }

    @GetMapping("/autocomplete")
    public java.util.List<String> autocomplete(
            @RequestParam("query") String query) {
        return bookService.autocomplete(query);
    }
}
