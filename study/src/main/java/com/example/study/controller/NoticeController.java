package com.example.study.controller;

import com.example.study.entity.Notice;
import com.example.study.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public List<Notice> list() {
        return noticeService.findAll();
    }

    @GetMapping("/{id}")
    public Notice get(@PathVariable Long id) {
        return noticeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Notice> create(@RequestBody Notice request) {
        Notice created = noticeService.create(request);
        return ResponseEntity.created(URI.create("/api/notices/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public Notice update(@PathVariable Long id, @RequestBody Notice request) {
        return noticeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}