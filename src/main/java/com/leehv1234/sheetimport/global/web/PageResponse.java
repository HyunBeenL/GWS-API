package com.leehv1234.sheetimport.global.web;

import org.springframework.data.domain.Page;

import java.util.List;

/** {@link Page}를 그대로 직렬화하면 내부 구조가 노출되므로 필요한 값만 담아 응답한다. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
