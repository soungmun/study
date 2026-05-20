package com.example.study.service;

import com.example.study.dto.response.NoticeDetailResponse;
import com.example.study.dto.response.NoticeListItem;
import com.example.study.entity.Notice;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
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

    public NoticeService(NoticeRepository noticeRepository,
                         CommentRepository commentRepository,
                         NoticeLikeService noticeLikeService) {
        this.noticeRepository = noticeRepository;
        this.commentRepository = commentRepository;
        this.noticeLikeService = noticeLikeService;
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
        return NoticeDetailResponse.of(n, c, l, iLiked);
    }

    @Transactional
    public NoticeDetailResponse increaseViewCount(Long id, Long currentUserId) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        n.increaseViewCount();
        long c = commentRepository.countByNoticeId(id);
        long l = noticeLikeService.count(id);
        boolean iLiked = noticeLikeService.liked(id, currentUserId);
        return NoticeDetailResponse.of(n, c, l, iLiked);
    }

    @Transactional
    public Notice create(Notice request, Long currentUserId) {
        Notice notice = new Notice(request.getAuthor(), request.getTitle(), request.getContent());
        notice.setAuthorId(currentUserId);
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice update(Long id, Notice request) {
        Notice notice = noticeRepository.findById(id)
                .orElse(new Notice());
        notice.setAuthor(request.getAuthor());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        return noticeRepository.save(notice);
    }

    @Transactional
    public void delete(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id);
        }
        noticeRepository.deleteById(id);
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
