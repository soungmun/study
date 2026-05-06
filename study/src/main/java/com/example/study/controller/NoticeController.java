package com.example.study.controller;

import com.example.study.entity.Notice;
import com.example.study.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public Page<Notice> list(
            @RequestParam(required = false, defaultValue = "title") String type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return noticeService.search(type, keyword, pageable);
    }

    @GetMapping("/{id}")
    public Notice get(@PathVariable Long id) {
        return noticeService.findById(id);
    }

    @PostMapping
    public Notice create(@Valid @RequestBody Notice request) {
        return noticeService.create(request);
    }

    @PutMapping("/{id}")
    public Notice update(@PathVariable Long id, @Valid @RequestBody Notice request) {
        return noticeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }
}