package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.request.NoticeRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.NoticeDetailResponse;
import com.example.study.dto.response.NoticeListItem;
import com.example.study.service.NoticeLikeService;
import com.example.study.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeLikeService noticeLikeService;

    public NoticeController(NoticeService noticeService, NoticeLikeService noticeLikeService) {
        this.noticeService = noticeService;
        this.noticeLikeService = noticeLikeService;
    }

    @GetMapping
    public Page<NoticeListItem> list(
            @RequestParam(value = "type", required = false, defaultValue = "title") String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return noticeService.search(type, keyword, pageable);
    }

    @GetMapping("/{id}")
    public NoticeDetailResponse get(@PathVariable Long id,
                                    @AuthenticationPrincipal SecurityUser principal) {
        return noticeService.findDetail(id, userId(principal));
    }

    @PostMapping("/{id}/views")
    public NoticeDetailResponse incrementView(@PathVariable Long id,
                                              @AuthenticationPrincipal SecurityUser principal) {
        return noticeService.increaseViewCount(id, userId(principal));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id,
                                        @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        return ResponseEntity.ok(noticeLikeService.toggle(id, principal.getUserId()));
    }

    @GetMapping("/{id}/likes")
    public java.util.List<com.example.study.dto.response.LikedUserResponse> likers(@PathVariable Long id) {
        return noticeLikeService.listLikers(id);
    }

    /**
     * 게시글 등록
     * Content-Type: application/json
     * Body: { "author": "...", "title": "...", "content": "...", "imageIds": [1, 2] }
     * imageIds 는 /api/notices/images 로 미리 업로드한 이미지의 id 목록 (없으면 빈 배열 or 생략)
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody NoticeRequest request,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        return ResponseEntity.ok(noticeService.create(request.toNotice(), principal.getUserId(), request.imageIds()));
    }

    /**
     * 게시글 수정
     * Body: { "author": "...", "title": "...", "content": "...", "imageIds": [3] }
     * imageIds 에 포함된 이미지는 이 게시글에 연결됩니다 (기존 이미지는 유지).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody NoticeRequest request,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        return ResponseEntity.ok(noticeService.update(id, request.toNotice(), principal.getUserId(), request.imageIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        noticeService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private Long userId(SecurityUser principal) {
        return principal != null ? principal.getUserId() : null;
    }
}
