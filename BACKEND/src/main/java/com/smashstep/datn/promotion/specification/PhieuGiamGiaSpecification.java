package com.smashstep.datn.promotion.specification;

import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class PhieuGiamGiaSpecification {

    public static Specification<PhieuGiamGia> ma(String ma) {
        return (root, query, cb) -> {

            if (ma == null || ma.trim().isEmpty()) {
                return null;
            }

            String keyword = "%" + ma.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("maPhieuGiamGia")), keyword),
                    cb.like(cb.lower(root.get("tenPhieuGiamGia")), keyword));
        };
    }

    public static Specification<PhieuGiamGia> tuNgay(LocalDate tuNgay) {
        return (root, query, cb) -> {

            if (tuNgay == null) {
                return null;
            }

            return cb.greaterThanOrEqualTo(
                    root.get("ngayBatDau"),
                    tuNgay.atStartOfDay()
            );
        };
    }

    public static Specification<PhieuGiamGia> denNgay(LocalDate denNgay) {
        return (root, query, cb) -> {

            if (denNgay == null) {
                return null;
            }

            return cb.lessThanOrEqualTo(
                    root.get("ngayBatDau"),
                    denNgay.atTime(23, 59, 59)
            );
        };
    }

    public static Specification<PhieuGiamGia> trangThai(Integer trangThai) {
        return (root, query, cb) -> {

            if (trangThai == null) {
                return null;
            }

            return cb.equal(
                    root.get("trangThai"),
                    trangThai
            );
        };
    }

    public static Specification<PhieuGiamGia> hinhThuc(
            Integer hinhThuc) {

        return (root, query, cb) -> {

            if (hinhThuc == null) {
                return null;
            }

            return cb.equal(root.get("hinhThucPhieu"), hinhThuc);
        };
    }

    public static Specification<PhieuGiamGia> loaiGiam(Integer loaiGiam) {
        return (root, query, cb) -> {
            if (loaiGiam == null) {
                return null;
            }

            return cb.equal(
                    root.get("loaiGiamGia"),
                    loaiGiam
            );
        };
    }

    public static Specification<PhieuGiamGia> notDeleted() {
        return (root, query, cb) -> cb.or(cb.isNull(root.get("trangThai")),
                cb.notEqual(root.get("trangThai"), -1));
    }
}
