package com.example.study.service;

import com.example.study.dto.response.NoticeDetailResponse;
import com.example.study.dto.response.NoticeListItem;
import com.example.study.entity.Notice;
import com.example.study.entity.NoticeImage; // NoticeImage import 추가
import com.example.study.entity.User; // User import 추가
import com.example.study.exception.ForbiddenException;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Collectors import 추가
import java.util.function.Function; // Function import 추가

/**
 * 공지사항(Notice)과 관련된 주요 비즈니스 로직을 담당하는 서비스 클래스입니다.
 * 공지사항의 CRUD(생성, 조회, 수정, 삭제) 및 검색 기능, 조회수 증가, 이미지 동기화 처리를 수행합니다.
 */
/**
 * 공지사항 게시판의 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 게시글 CRUD 연산 및 조회수 증가, 목록 검색 기능 등을 수행합니다.
 */
@Service
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final CommentRepository commentRepository;
    private final NoticeLikeService noticeLikeService;
    private final UserRepository userRepository;
    private final NoticeImageService noticeImageService; // NoticeImageService는 여전히 필요 (업로드, 파일 삭제 등)

    public NoticeService(NoticeRepository noticeRepository,
                         CommentRepository commentRepository,
                         NoticeLikeService noticeLikeService,
                         UserRepository userRepository,
                         NoticeImageService noticeImageService) {
        this.noticeRepository = noticeRepository;
        this.commentRepository = commentRepository;
        this.noticeLikeService = noticeLikeService;
        this.userRepository = userRepository;
        this.noticeImageService = noticeImageService;
    }

    /**
     * 공지사항 목록을 검색 및 페이징하여 반환합니다.
     * @param type 검색 조건 (예: "content" 등)
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return 검색된 공지사항 목록 (작성자 정보, 댓글 수, 좋아요 수가 포함된 DTO)
     */
    public Page<NoticeListItem> search(String type, String keyword, Pageable pageable) {
        Page<Notice> page;
        if (keyword == null || keyword.isBlank()) { // keyword == null 중복 제거
            page = noticeRepository.findAll(pageable);
        } else if ("content".equalsIgnoreCase(type)) {
            page = noticeRepository.findByContentLike(keyword, pageable);
        } else {
            // findByTitleContaining 대신 findByTitleLike 사용
            page = noticeRepository.findByTitleLike(keyword, pageable);
        }
        return enrich(page);
    }

    /**
     * 특정 공지사항의 상세 정보를 조회합니다.
     * @param id 공지사항 ID
     * @param currentUserId 현재 로그인한 사용자 ID (좋아요 여부 및 수정 권한 확인용)
     * @return 공지사항 상세 정보 (이미지, 댓글 수, 좋아요 수 등 포함)
     */
    public NoticeDetailResponse findDetail(Long id, Long currentUserId) {
        // Notice를 가져올 때 이미지를 EAGER로 가져오거나, LAZY라면 getImages() 호출 시 로딩
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        long c = commentRepository.countByNoticeId(id);
        long l = noticeLikeService.count(id);
        boolean iLiked = noticeLikeService.liked(id, currentUserId);
        User authorUser = userRepository.findById(n.getAuthorId()).orElse(null); // 작성자 정보 가져오기

        // Notice 엔티티의 images 컬렉션에서 직접 이미지 정보 추출
        List<String> imageUrls = n.getImages().stream()
                .map(img -> noticeImageService.getImageUrl(img.getStoredName())) // 변경된 메서드 호출
                .collect(Collectors.toList());
        List<NoticeImageService.ImageInfo> images = n.getImages().stream()
                .map(img -> new NoticeImageService.ImageInfo(
                        img.getId(),
                        noticeImageService.getImageUrl(img.getStoredName()), // 변경된 메서드 호출
                        img.getOriginalName()
                ))
                .collect(Collectors.toList());

        return NoticeDetailResponse.of(n, authorUser, c, l, iLiked, canModify(n, currentUserId), imageUrls, images);
    }

    /**
     * 공지사항의 조회수를 1 증가시키고 상세 정보를 반환합니다.
     * @param id 공지사항 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 공지사항 상세 정보
     */
    @Transactional
    public NoticeDetailResponse increaseViewCount(Long id, Long currentUserId) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        n.increaseViewCount();
        long c = commentRepository.countByNoticeId(id);
        long l = noticeLikeService.count(id);
        boolean iLiked = noticeLikeService.liked(id, currentUserId);
        User authorUser = userRepository.findById(n.getAuthorId()).orElse(null); // 작성자 정보 가져오기

        // Notice 엔티티의 images 컬렉션에서 직접 이미지 정보 추출
        List<String> imageUrls = n.getImages().stream()
                .map(img -> noticeImageService.getImageUrl(img.getStoredName())) // 변경된 메서드 호출
                .collect(Collectors.toList());
        List<NoticeImageService.ImageInfo> images = n.getImages().stream()
                .map(img -> new NoticeImageService.ImageInfo(
                        img.getId(),
                        noticeImageService.getImageUrl(img.getStoredName()), // 변경된 메서드 호출
                        img.getOriginalName()
                ))
                .collect(Collectors.toList());

        return NoticeDetailResponse.of(n, authorUser, c, l, iLiked, canModify(n, currentUserId), imageUrls, images);
    }

    /**
     * 새로운 공지사항을 등록합니다. (관리자 권한 필요)
     * @param request 등록할 공지사항 데이터
     * @param currentUserId 작성자(현재 로그인한 관리자) ID
     * @param imageIds 함께 등록할 첨부 이미지 ID 목록
     * @return 등록된 공지사항 엔티티
     */
    @Transactional
    public Notice create(Notice request, Long currentUserId, List<Long> imageIds) {
        if (!isAdmin(currentUserId)) {
            throw new ForbiddenException("관리자만 공지를 등록할 수 있습니다.");
        }
        User authorUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다. id=" + currentUserId));

        Notice notice = new Notice(authorUser.getUsername(), request.getTitle(), request.getContent()); // username 사용
        notice.setAuthorId(currentUserId);
        Notice saved = noticeRepository.save(notice); // Notice 저장 -> id가 생성됨

        // imageIds에 해당하는 NoticeImage들을 찾아 Notice에 연결
        if (imageIds != null && !imageIds.isEmpty()) {
            List<NoticeImage> imagesToAttach = noticeImageService.findImagesByIds(imageIds, currentUserId); // 변경된 메서드 호출
            for (NoticeImage image : imagesToAttach) {
                saved.addImage(image); // Notice의 편의 메서드를 사용하여 양방향 관계 설정
            }
            // saved.getImages() 컬렉션에 추가된 이미지들은 cascade = ALL 에 의해 함께 저장됨
        }
        return saved;
    }

    /**
     * 기존 공지사항을 수정합니다. (작성자 본인 또는 관리자 권한 필요)
     * 변경된 이미지 목록을 비교하여 삭제된 이미지는 DB와 파일 시스템에서 제거하고, 새로운 이미지를 추가합니다.
     *
     * @param id 수정할 공지사항 ID
     * @param request 수정할 내용이 담긴 데이터
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param imageIds 최종적으로 유지/추가할 첨부 이미지 ID 목록
     * @return 수정된 공지사항 엔티티
     */
    @Transactional
    public Notice update(Long id, Notice request, Long currentUserId, List<Long> imageIds) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!canModify(notice, currentUserId)) {
            throw new ForbiddenException("본인 또는 관리자만 수정할 수 있습니다.");
        }
        User authorUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다. id=" + currentUserId));

        notice.setAuthor(authorUser.getUsername()); // username 사용
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());

        // 이미지 동기화 로직
        // 1. 현재 Notice에 연결된 이미지 ID 목록
        List<Long> currentImageIds = notice.getImages().stream()
                .map(NoticeImage::getId)
                .collect(Collectors.toList());

        // 2. 삭제할 이미지: currentImageIds에는 있지만 keepImageIds(imageIds)에는 없는 것
        List<NoticeImage> imagesToDelete = notice.getImages().stream()
                .filter(img -> !imageIds.contains(img.getId()))
                .collect(Collectors.toList());
        for (NoticeImage img : imagesToDelete) {
            notice.removeImage(img); // Notice에서 이미지 제거 (orphanRemoval=true에 의해 DB에서도 삭제)
            noticeImageService.deleteFile(img.getStoredName()); // 실제 파일 삭제
        }

        // 3. 추가할 이미지: keepImageIds(imageIds)에는 있지만 currentImageIds에는 없는 것
        List<Long> newImageIds = imageIds.stream()
                .filter(imageId -> !currentImageIds.contains(imageId))
                .collect(Collectors.toList());
        if (!newImageIds.isEmpty()) {
            List<NoticeImage> imagesToAdd = noticeImageService.findImagesByIds(newImageIds, currentUserId); // 변경된 메서드 호출
            for (NoticeImage img : imagesToAdd) {
                notice.addImage(img); // Notice에 이미지 추가
            }
        }
        // Notice 엔티티를 저장하면 변경된 images 컬렉션도 함께 반영됨
        return noticeRepository.save(notice);
    }

    /**
     * 공지사항을 삭제합니다. (작성자 본인 또는 관리자 권한 필요)
     * 삭제 시 공지사항에 연결된 이미지 파일들도 파일 시스템에서 모두 제거됩니다.
     *
     * @param id 삭제할 공지사항 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!canModify(notice, currentUserId)) {
            throw new ForbiddenException("본인 또는 관리자만 삭제할 수 있습니다.");
        }
        // NoticeImageService를 통해 연결된 이미지 파일들을 먼저 삭제
        // Notice 엔티티의 images 컬렉션을 통해 이미지 파일명을 얻어와 삭제
        notice.getImages().forEach(img -> noticeImageService.deleteFile(img.getStoredName()));

        // Notice를 삭제하면 cascade = ALL, orphanRemoval = true 설정에 의해 연결된 NoticeImage들도 자동으로 삭제됨
        noticeRepository.delete(notice);
    }

    /** 본인(작성자) 또는 관리자인지 — 수정/삭제 권한 판별. */
    private boolean canModify(Notice notice, Long currentUserId) {
        if (currentUserId == null) return false;
        if (currentUserId.equals(notice.getAuthorId())) return true;
        return isAdmin(currentUserId);
    }

    /** role == ADMIN 인지 — 등록 권한 판별. */
    private boolean isAdmin(Long currentUserId) {
        if (currentUserId == null) return false;
        return userRepository.findById(currentUserId)
                .map(u -> "ADMIN".equals(u.getRole()))
                .orElse(false);
    }

    /** Page<Notice> → Page<NoticeListItem> 변환 + 댓글/좋아요 카운트 일괄 join */
    private Page<NoticeListItem> enrich(Page<Notice> page) {
        List<Notice> content = page.getContent();
        if (content.isEmpty()) return page.map(n -> NoticeListItem.of(n, null, 0, 0)); // null 대신 User 객체 전달

        // 모든 작성자 ID를 수집
        List<Long> authorIds = content.stream()
                .map(Notice::getAuthorId)
                .filter(java.util.Objects::nonNull) // authorId가 null이 아닌 경우만 필터링
                .distinct()
                .toList();

        // 작성자 정보를 일괄 조회
        Map<Long, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> ids = content.stream().map(Notice::getId).toList();
        Map<Long, Long> commentMap = countMap(commentRepository.countGroupByNoticeIds(ids));
        Map<Long, Long> likeMap = noticeLikeService.countsByNoticeIds(ids);

        return page.map(n -> NoticeListItem.of(
                n,
                authorMap.get(n.getAuthorId()), // 조회된 User 객체를 전달
                commentMap.getOrDefault(n.getId(), 0L),
                likeMap.getOrDefault(n.getId(), 0L)
        ));
    }

    private Map<Long, Long> countMap(Collection<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put((Long) r[0], (Long) r[1]);
        return m;
    }

    /** User 객체로부터 표시 이름 (닉네임 또는 사용자 이름)을 가져오는 헬퍼 메서드 */
    private String displayName(User u) {
        return u.getNickname() != null ? u.getNickname() : u.getUsername();
    }
}
