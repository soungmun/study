package com.example.study.service;

import com.example.study.entity.Notice;
import com.example.study.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public Page<Notice> findAll(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    @Transactional
    public Notice findById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        notice.increaseViewCount();
        return notice;
    }

    @Transactional
    public Notice create(Notice request) {
        Notice notice = new Notice(request.getAuthor(), request.getTitle(), request.getContent());
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice update(Long id, Notice request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        notice.update(request.getAuthor(), request.getTitle(), request.getContent());
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id);
        }
        noticeRepository.deleteById(id);
    }
}