package com.example.study.controller;

import com.example.study.dto.response.BookSearchResponse;
import com.example.study.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
