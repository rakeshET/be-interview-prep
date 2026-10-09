package com.edstem.interviewprep.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON for paged results. Serialising Spring's {@code PageImpl} directly is discouraged: its
 * shape is an implementation detail that can change between versions.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.getSort().isSorted() ? page.getSort().toString() : null);
    }
}
