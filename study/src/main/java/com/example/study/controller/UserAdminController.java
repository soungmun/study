package com.example.study.controller;

import com.example.study.dto.response.MessageResponse; // MessageResponse import 추가
import com.example.study.dto.response.UserListItem;
import com.example.study.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // RestController, RequestMapping, GetMapping 외에 DeleteMapping 추가

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

    // 모든 사용자 삭제 API 추가
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')") // 오타 수정 완료
    public ResponseEntity<MessageResponse> deleteAllUsers() {
        userRepository.deleteAll(); // 모든 User 엔티티 삭제
        return ResponseEntity.ok(MessageResponse.of("모든 사용자가 삭제되었습니다."));
    }
}
