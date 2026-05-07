package com.example.study.controller;

import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.UserListItem;
import com.example.study.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> list(HttpSession session) {
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long)) {
            return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        }
        List<UserListItem> items = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserListItem::from)
                .toList();
        return ResponseEntity.ok(items);
    }
}
