package com.smashstep.datn.promotion.specification;

import com.smashstep.datn.promotion.entity.DotGiamGia;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DotGiamGiaSpecification {

    private DotGiamGiaSpecification() {
    }

    public static Specification<DotGiamGia> filter(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            Integer trangThai
    ) {

        return (root, query, cb) -> {

            var predicate = cb.conjunction();

            // =========================================
            // KHÔNG LẤY CÁC ĐỢT ĐÃ XÓA MỀM
            // -1 = Đã xóa
            // =========================================
            predicate = cb.and(
                    predicate,
                    cb.notEqual(
                            root.get("trangThai"),
                            -1
                    )
            );

            // =========================================
            // TÌM THEO MÃ HOẶC TÊN
            // =========================================
            if (ma != null && !ma.trim().isEmpty()) {

                String keyword =
                        "%" + ma.trim().toLowerCase() + "%";

                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.like(
                                        cb.lower(
                                                root.get("maDotGiamGia")
                                        ),
                                        keyword
                                ),
                                cb.like(
                                        cb.lower(
                                                root.get("tenDotGiamGia")
                                        ),
                                        keyword
                                )
                        )
                );
            }

            // =========================================
            // NGÀY BẮT ĐẦU >= TỪ NGÀY
            // =========================================
            if (tuNgay != null) {

                LocalDateTime from =
                        tuNgay.atStartOfDay();

                predicate = cb.and(
                        predicate,
                        cb.greaterThanOrEqualTo(
                                root.get("ngayBatDau"),
                                from
                        )
                );
            }

            // =========================================
            // NGÀY KẾT THÚC <= ĐẾN NGÀY
            // =========================================
            if (denNgay != null) {

                LocalDateTime to =
                        denNgay.atTime(
                                23,
                                59,
                                59
                        );

                predicate = cb.and(
                        predicate,
                        cb.lessThanOrEqualTo(
                                root.get("ngayKetThuc"),
                                to
                        )
                );
            }

            // =========================================
            // LỌC TRẠNG THÁI
            // 1 = Đang hoạt động
            // 0 = Ngừng hoạt động
            // =========================================
            if (trangThai != null) {

                predicate = cb.and(
                        predicate,
                        cb.equal(
                                root.get("trangThai"),
                                trangThai
                        )
                );
            }

            return predicate;
        };
    }
}