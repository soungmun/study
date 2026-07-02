package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.response.MessageResponse;
import com.example.study.entity.NoticeImage;
import com.example.study.service.NoticeImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 공지사항 게시판 이미지 처리 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 이미지 파일 업로드 및 서빙 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/notices")
public class NoticeImageController {

    private final NoticeImageService imageService;

    public NoticeImageController(NoticeImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 이미지 업로드 (게시글 작성 전에 미리 업로드)
     * POST /api/notices/images
     * Content-Type: multipart/form-data
     * 파라미터: file (이미지 파일)
     *
     * 응답: { "imageId": 1, "url": "http://localhost:8080/uploads/notice-images/uuid.jpg" }
     * 게시글 저장 시 imageIds 배열에 이 id를 포함하면 연결됩니다.
     */
    @PostMapping("/images")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal SecurityUser principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        }

        try {
            NoticeImage saved = imageService.upload(file, principal.getUserId());
            // 변경된 메서드 이름으로 수정
            String url = imageService.getImageUrl(saved.getStoredName());

            return ResponseEntity.ok(Map.of(
                    "imageId", saved.getId(),
                    "url", url,
                    "originalName", saved.getOriginalName(),
                    "fileSize", saved.getFileSize()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(MessageResponse.of(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(MessageResponse.of("파일 저장 중 오류가 발생했습니다."));
        }
    }
}
