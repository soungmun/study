package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.response.NoticeDetailResponse;
import com.example.study.dto.response.NoticeListItem;
import com.example.study.service.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Notice JSP 뷰 전용 MVC Controller.
 * REST API(/api/notices)와 별개로 동작하며 /notices/** 경로로 JSP 페이지를 서빙합니다.
 */
@Controller
@RequestMapping("/notices")
public class NoticeViewController {

    private final NoticeService noticeService;

    public NoticeViewController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** 공지사항 목록 */
    @GetMapping
    public String list(
            @RequestParam(value = "type", required = false, defaultValue = "title") String type,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model) {

        Page<NoticeListItem> noticePage = noticeService.search(
                type, keyword.isBlank() ? null : keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));

        model.addAttribute("noticePage", noticePage);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        return "notice/list";
    }

    /** 공지사항 상세 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal SecurityUser principal,
                         Model model) {
        Long userId = principal != null ? principal.getUserId() : null;
        // 조회수 증가
        NoticeDetailResponse notice = noticeService.increaseViewCount(id, userId);
        model.addAttribute("notice", notice);
        model.addAttribute("isLoggedIn", principal != null);
        model.addAttribute("isAdmin", principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return "notice/detail";
    }

    /** 공지사항 작성 폼 (관리자만) */
    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal SecurityUser principal, Model model) {
        if (principal == null) return "redirect:/notices";
        model.addAttribute("mode", "create");
        return "notice/form";
    }

    /** 공지사항 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal SecurityUser principal,
                           Model model) {
        if (principal == null) return "redirect:/notices";
        Long userId = principal.getUserId();
        NoticeDetailResponse notice = noticeService.findDetail(id, userId);
        if (!notice.canEdit()) return "redirect:/notices/" + id;
        model.addAttribute("notice", notice);
        model.addAttribute("mode", "edit");
        return "notice/form";
    }
}
