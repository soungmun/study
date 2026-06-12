package com.example.study.service;

import com.example.study.entity.NoticeImage;
import com.example.study.repository.NoticeImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NoticeImageService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final NoticeImageRepository imageRepository;
    private final String uploadDir;
    private final String serverUrl;

    public NoticeImageService(
            NoticeImageRepository imageRepository,
            @Value("${app.upload.dir:uploads/notice-images}") String uploadDir,
            @Value("${app.server.url:http://localhost:8080}") String serverUrl) {
        this.imageRepository = imageRepository;
        this.uploadDir = uploadDir;
        this.serverUrl = serverUrl;
    }

    /**
     * 이미지 업로드 — 파일을 디스크에 저장하고 DB에 기록.
     * 게시글 저장 전 미리 업로드할 수 있으며, 이때 noticeId는 null.
     */
    @Transactional
    public NoticeImage upload(MultipartFile file, Long userId) throws IOException {
        validateFile(file);

        // 저장 디렉토리 생성
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        // UUID 기반 파일명으로 저장 (원본명 충돌 방지)
        String ext = extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + ext;
        Path dest = dir.resolve(storedName);
        file.transferTo(dest);

        NoticeImage image = new NoticeImage(
                userId,
                storedName,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );
        return imageRepository.save(image);
    }

    /**
     * 게시글 최초 저장 시 이미지들을 해당 noticeId에 연결.
     * imageIds 중 해당 userId가 업로드한 것만 연결 (보안).
     */
    @Transactional
    public void attachImages(Long noticeId, List<Long> imageIds, Long userId) {
        if (imageIds == null || imageIds.isEmpty()) return;
        List<NoticeImage> images = imageRepository.findByIdInAndUserId(imageIds, userId);
        images.forEach(img -> img.attachToNotice(noticeId));
    }

    /**
     * 게시글 수정 시 이미지 동기화.
     * - keepImageIds 에 없는 기존 이미지는 연결 해제(noticeId=null)
     * - keepImageIds 에 있지만 아직 연결 안 된 이미지는 연결
     * keepImageIds = 프론트에서 최종적으로 남길 imageId 전체 목록
     */
    @Transactional
    public void syncImages(Long noticeId, List<Long> keepImageIds) {
        // 현재 연결된 이미지
        List<NoticeImage> current = imageRepository.findByNoticeIdOrderByIdAsc(noticeId);

        if (keepImageIds == null || keepImageIds.isEmpty()) {
            // 모두 연결 해제
            current.forEach(img -> img.attachToNotice(null));
            return;
        }

        // 제거 대상: 현재 연결됐지만 keepImageIds에 없는 것
        current.stream()
                .filter(img -> !keepImageIds.contains(img.getId()))
                .forEach(img -> img.attachToNotice(null));

        // 추가 대상: keepImageIds에 있지만 아직 연결 안 된 것 (새로 업로드한 이미지)
        List<Long> currentIds = current.stream().map(NoticeImage::getId).toList();
        List<Long> newIds = keepImageIds.stream().filter(id -> !currentIds.contains(id)).toList();
        if (!newIds.isEmpty()) {
            List<NoticeImage> newImages = imageRepository.findByIdIn(newIds);
            newImages.forEach(img -> img.attachToNotice(noticeId));
        }
    }

    /** 업로드 직후 단일 이미지 URL 반환 */
    public String getUploadedImageUrl(String storedName) {
        return serverUrl + "/uploads/notice-images/" + storedName;
    }

    /** 게시글에 연결된 이미지 URL 목록 반환 */
    public List<String> getImageUrls(Long noticeId) {
        return imageRepository.findByNoticeIdOrderByIdAsc(noticeId)
                .stream()
                .map(img -> serverUrl + "/uploads/notice-images/" + img.getStoredName())
                .toList();
    }

    /** 게시글에 연결된 이미지 ID + URL 쌍 목록 반환 (수정 화면용) */
    public List<ImageInfo> getImages(Long noticeId) {
        return imageRepository.findByNoticeIdOrderByIdAsc(noticeId)
                .stream()
                .map(img -> new ImageInfo(
                        img.getId(),
                        serverUrl + "/uploads/notice-images/" + img.getStoredName(),
                        img.getOriginalName()
                ))
                .toList();
    }

    /** 이미지 ID + URL + 원본명을 담는 간단한 DTO */
    public record ImageInfo(Long imageId, String url, String originalName) {}

    /** 게시글 삭제 시 연결된 이미지도 삭제 */
    @Transactional
    public void deleteByNoticeId(Long noticeId) {
        List<NoticeImage> images = imageRepository.findByNoticeIdOrderByIdAsc(noticeId);
        images.forEach(img -> deleteFile(img.getStoredName()));
        imageRepository.deleteByNoticeId(noticeId);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 형식입니다. JPEG, PNG, GIF, WEBP만 허용합니다.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private void deleteFile(String storedName) {
        try {
            Path path = Paths.get(uploadDir).resolve(storedName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 파일 삭제 실패는 로그만 남기고 DB 삭제는 진행
        }
    }
}
