package com.smashstep.datn.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Dữ liệu phân trang trả trong ApiResponse.data cho các API danh sách.
 * Lưu ý: page tính từ 1 (khớp với FE), còn Spring Data tính từ 0.
 *
 * Cách dùng trong controller:
 *   Page<HoaDonDto> p = repo.findAll(PageRequest.of(page - 1, size)).map(this::toDto);
 *   return ApiResponse.ok(PageResponse.from(p));
 */
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
