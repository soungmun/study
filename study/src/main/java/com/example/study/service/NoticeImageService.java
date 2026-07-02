package com.example.study.service;

import com.example.study.entity.NoticeImage;
import com.example.study.repository.NoticeImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * 공지사항 이미지 파일 업로드 및 삭제 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 로컬 파일 시스템 또는 클라우드 스토리지에 이미지를 저장하고 관리합니다.
 */
@Service
@Transactional(readOnly = true)
public class NoticeImageService {

    private static final Logger log = LoggerFactory.getLogger(NoticeImageService.class);
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
     * 게시글 저장 전 미리 업로드할 수 있으며, 이때 notice는 null.
     */
    @Transactional
    public NoticeImage upload(MultipartFile file, Long userId) throws IOException {
        validateFile(file);

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

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
     * imageIds에 해당하는 NoticeImage들을 조회.
     * NoticeService에서 Notice에 연결할 때 사용.
     */
    public List<NoticeImage> findImagesByIds(List<Long> imageIds, Long userId) {
        if (imageIds == null || imageIds.isEmpty()) return List.of();
        return imageRepository.findByIdInAndUserId(imageIds, userId);
    }

    /** 업로드된 이미지의 URL 반환 */
    public String getImageUrl(String storedName) {
        return serverUrl + "/uploads/notice-images/" + storedName;
    }

    /** 이미지 ID + URL + 원본명을 담는 간단한 DTO */
    public record ImageInfo(Long imageId, String url, String originalName) {}

    /** 회원 탈퇴 시 해당 사용자가 업로드한 모든 이미지 삭제 (파일 + DB) */
    @Transactional
    public void deleteByUserId(Long userId) {
        List<NoticeImage> images = imageRepository.findByUserId(userId);
        for (NoticeImage image : images) {
            deleteFile(image.getStoredName());
        }
        imageRepository.deleteAll(images);
    }

    /** 실제 파일 시스템에서 이미지 파일 삭제 */
    public void deleteFile(String storedName) {
        try {
            Path path = Paths.get(uploadDir).resolve(storedName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {} - {}", storedName, e.getMessage());
        }
    }

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
}
