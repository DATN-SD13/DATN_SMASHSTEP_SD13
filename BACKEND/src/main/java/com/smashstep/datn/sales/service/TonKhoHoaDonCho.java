package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/** Invoice row + stock changes commit together. Caller holds invoice creation lock then invoice lock. */
public final class TonKhoHoaDonCho {
    private TonKhoHoaDonCho() {}
    public static Map<Long, Integer> soLuong(List<HoaDonChiTiet> rows) {
        Map<Long, Integer> result = new TreeMap<>();
        for (var row : rows) if (row.getIdSanPhamChiTiet() != null && row.getSoLuong() != null)
            result.merge(row.getIdSanPhamChiTiet().getId(), row.getSoLuong(), Math::addExact);
        return result;
    }
    public static Map<Long, SanPhamChiTiet> khoaBienThe(EntityManager em, Collection<Long> ids) {
        var rows = ids.stream().distinct().map(id -> em.find(SanPhamChiTiet.class, id))
                .filter(Objects::nonNull).sorted(Comparator
                        .comparing((SanPhamChiTiet v) -> v.getIdSanPham() == null ? Long.MAX_VALUE : v.getIdSanPham().getId())
                        .thenComparing(SanPhamChiTiet::getId)).toList();
        Map<Long, SanPhamChiTiet> result = new TreeMap<>();
        for (var v : rows) {
            if (v.getIdSanPham() != null) {
                em.lock(v.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                em.refresh(v.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
            }
            em.refresh(v, LockModeType.PESSIMISTIC_WRITE);
            result.put(v.getId(), v);
        }
        return result;
    }
    public static int soLuongSauLuu(int hienTai, int cu, int moi, boolean daTruKho) {
        long result = (long) hienTai + (daTruKho ? cu : 0) - moi;
        if (result < 0 || result > Integer.MAX_VALUE) throw AppException.conflict("Tồn kho không đủ hoặc vượt giới hạn");
        return (int) result;
    }
    public static void capNhat(HoaDon h, Map<Long, Integer> cu, Map<Long, Integer> moi, Map<Long, SanPhamChiTiet> variants) {
        Map<Long, Integer> stocks = new TreeMap<>();
        for (var entry : variants.entrySet()) stocks.put(entry.getKey(), soLuongSauLuu(
                entry.getValue().getSoLuong() == null ? 0 : entry.getValue().getSoLuong(),
                cu.getOrDefault(entry.getKey(), 0), moi.getOrDefault(entry.getKey(), 0), PosDraftRules.daTruKho(h)));
        for (var entry : stocks.entrySet()) {
            var v = variants.get(entry.getKey()); v.setSoLuong(entry.getValue()); v.setNgayCapNhat(LocalDateTime.now());
        }
        PosDraftRules.danhDauTruKho(h);
    }
    public static void hoanKhiHuy(EntityManager em, HoaDon h) {
        if (!PosDraftRules.daTruKho(h) || !Integer.valueOf(8).equals(h.getTrangThai()) || h.getNgayThanhToan() != null) return;
        var rows = em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id = :id", HoaDonChiTiet.class)
                .setParameter("id", h.getId()).getResultList();
        var cu = soLuong(rows);
        capNhat(h, cu, Map.of(), khoaBienThe(em, cu.keySet()));
    }
}
