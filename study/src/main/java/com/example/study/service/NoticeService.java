package com.example.study.service;

import com.example.study.dto.NoticeRequest;
import com.example.study.dto.NoticeResponse;
import com.example.study.entity.Notice;
import com.example.study.repository.NoticeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<NoticeResponse> findAll() {
        return noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(NoticeResponse::new)
                .toList();
    }

    public NoticeResponse findById(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        return new NoticeResponse(notice);
    }

    @Transactional
    public NoticeResponse create(NoticeRequest request) {
        Notice notice = new Notice(request.getAuthor(), request.getTitle(), request.getContent());
        return new NoticeResponse(noticeRepository.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        notice.update(request.getAuthor(), request.getTitle(), request.getContent());
        return new NoticeResponse(notice);
    }

    @Transactional
    public void delete(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id);
        }
        noticeRepository.deleteById(id);
    }
}