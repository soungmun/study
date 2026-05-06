package com.example.study.controller;

import com.example.study.entity.Notice;
import com.example.study.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/notices")
public class NoticeViewController {

    private final NoticeService noticeService;

    public NoticeViewController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }


    @GetMapping
    public String list(
            @RequestParam(required = false, defaultValue = "title") String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Notice> notices = noticeService.search(type, keyword, pageable);
        model.addAttribute("notices", notices);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        return "notice/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("notice", new Notice());
        return "notice/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("notice") Notice notice,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "notice/form";
        }
        Notice saved = noticeService.create(notice);
        return "redirect:/notices/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.findById(id));
        return "notice/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.findById(id));
        return "notice/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("notice") Notice notice,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            notice.setId(id);
            return "notice/form";
        }
        noticeService.update(id, notice);
        return "redirect:/notices/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        noticeService.delete(id);
        return "redirect:/notices";
    }
}