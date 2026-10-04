package com.smashstep.datn.product.service;

import com.smashstep.datn.common.exception.AppException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Locale;
import java.util.Objects;

public final class QuyTacSanPham {
    private QuyTacSanPham() {
    }

    public static String boKhoangTrang(String giaTri) {
        return giaTri == null ? "" : giaTri.trim();
    }

    public static void kiemTraTrangThai(Integer trangThai) {
        if (trangThai != null && trangThai != 0 && trangThai != 1) {
            throw AppException.badRequest("Trạng thái phải là 0 hoặc 1");
        }
    }

    public static PageRequest taoPhanTrang(int trang, int kichThuocTrang) {
        if (trang < 0 || kichThuocTrang < 1 || kichThuocTrang > 100) {
            throw AppException.badRequest("Trang phải từ 0, kích thước trang từ 1 đến 100");
        }
        return PageRequest.of(trang, kichThuocTrang, Sort.by(Sort.Direction.DESC, "id"));
    }

    public static String taoMauTimKiem(String tuKhoa) {
        return "%" + boKhoangTrang(tuKhoa).toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
    }

    static void kiemTraThuocTinhHoatDong(Integer trangThai, Long idCu, Long idMoi) {
        if (!Objects.equals(trangThai, 1) && !Objects.equals(idCu, idMoi)) {
            throw AppException.badRequest("Thuộc tính được chọn đã ngừng hoạt động");
        }
    }
}
