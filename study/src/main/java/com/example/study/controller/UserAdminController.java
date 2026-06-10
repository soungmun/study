package com.example.study.controller;

import com.example.study.dto.response.UserListItem;
import com.example.study.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserListItem>> list() {
        List<UserListItem> items = userRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserListItem::from)
                .toList();
        return ResponseEntity.ok(items);
    }
}
