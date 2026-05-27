package com.example.study.service;

import com.example.study.dto.response.NoticeDetailResponse;
import com.example.study.dto.response.NoticeListItem;
import com.example.study.entity.Notice;
import com.example.study.exception.ForbiddenException;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final CommentRepository commentRepository;
    private final NoticeLikeService noticeLikeService;
    private final UserRepository userRepository;
    private final String adminUsername;

    public NoticeService(NoticeRepository noticeRepository,
                         CommentRepository commentRepository,
                         NoticeLikeService noticeLikeService,
                         UserRepository userRepository,
                         @Value("${app.admin.username:}") String adminUsername) {
        this.noticeRepository = noticeRepository;
        this.commentRepository = commentRepository;
        this.noticeLikeService = noticeLikeService;
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    public Page<NoticeListItem> search(String type, String keyword, Pageable pageable) {
        Page<Notice> page;
        if (keyword == null || keyword.isBlank()) {
            page = noticeRepository.findAll(pageable);
        } else if ("content".equalsIgnoreCase(type)) {
            page = noticeRepository.findByContentContaining(keyword, pageable);
        } else {
            page = noticeRepository.findByTitleContaining(keyword, pageable);
        }
        return enrich(page);
    }

    public NoticeDetailResponse findDetail(Long id, Long currentUserId) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        long c = commentRepository.countByNoticeId(id);
        long l = noticeLikeService.count(id);
        boolean iLiked = noticeLikeService.liked(id, currentUserId);
        return NoticeDetailResponse.of(n, c, l, iLiked, canModify(n, currentUserId));
    }

    @Transactional
    public NoticeDetailResponse increaseViewCount(Long id, Long currentUserId) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        n.increaseViewCount();
        long c = commentRepository.countByNoticeId(id);
        long l = noticeLikeService.count(id);
        boolean iLiked = noticeLikeService.liked(id, currentUserId);
        return NoticeDetailResponse.of(n, c, l, iLiked, canModify(n, currentUserId));
    }

    @Transactional
    public Notice create(Notice request, Long currentUserId) {
        if (!isAdmin(currentUserId)) {
            throw new ForbiddenException("관리자만 공지를 등록할 수 있습니다.");
        }
        Notice notice = new Notice(request.getAuthor(), request.getTitle(), request.getContent());
        notice.setAuthorId(currentUserId);
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice update(Long id, Notice request, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!canModify(notice, currentUserId)) {
            throw new ForbiddenException("본인 또는 관리자만 수정할 수 있습니다.");
        }
        notice.setAuthor(request.getAuthor());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        return noticeRepository.save(notice);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!canModify(notice, currentUserId)) {
            throw new ForbiddenException("본인 또는 관리자만 삭제할 수 있습니다.");
        }
        noticeRepository.delete(notice);
    }

    /** 본인(작성자) 또는 관리자인지 — 수정/삭제 권한 판별. */
    private boolean canModify(Notice notice, Long currentUserId) {
        if (currentUserId == null) return false;
        if (currentUserId.equals(notice.getAuthorId())) return true;
        return isAdmin(currentUserId);
    }

    /** 관리자(app.admin.username)인지 — 등록 권한 판별. */
    private boolean isAdmin(Long currentUserId) {
        if (currentUserId == null) return false;
        if (adminUsername == null || adminUsername.isBlank()) return false;
        return userRepository.findById(currentUserId)
                .map(u -> adminUsername.equals(u.getUsername()))
                .orElse(false);
    }

    /** Page<Notice> → Page<NoticeListItem> 변환 + 댓글/좋아요 카운트 일괄 join */
    private Page<NoticeListItem> enrich(Page<Notice> page) {
        List<Notice> content = page.getContent();
        if (content.isEmpty()) return page.map(n -> NoticeListItem.of(n, 0, 0));

        List<Long> ids = content.stream().map(Notice::getId).toList();
        Map<Long, Long> commentMap = countMap(commentRepository.countGroupByNoticeIds(ids));
        Map<Long, Long> likeMap = noticeLikeService.countsByNoticeIds(ids);

        return page.map(n -> NoticeListItem.of(
                n,
                commentMap.getOrDefault(n.getId(), 0L),
                likeMap.getOrDefault(n.getId(), 0L)
        ));
    }

    private Map<Long, Long> countMap(Collection<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put((Long) r[0], (Long) r[1]);
        return m;
    }
}
