package com.example.study.dto.request;

import com.example.study.entity.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoticeRequest(

        @NotBlank(message = "작성자를 입력하세요.")
        @Size(max = 100, message = "작성자는 100자 이하여야 합니다.")
        String author,

        @NotBlank(message = "제목을 입력하세요.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력하세요.")
        String content,

        /** 미리 업로드한 이미지 ID 목록. 없으면 null 또는 빈 리스트. */
        List<Long> imageIds

) {
    /** 엔티티 변환 헬퍼 */
    public Notice toNotice() {
        return new Notice(author, title, content);
    }
}
