package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

/** Inventory for web orders is deducted at placement. The creation history is the audit marker. */
public final class OnlineOrderInventory {
    public static final String PREFIX="[WEB-ORDER:1]|";
    private OnlineOrderInventory() {}
    public static boolean managed(EntityManager em,HoaDon h) {
        return Integer.valueOf(1).equals(h.getLoaiHoaDon()) && em.createQuery(
                "select count(l) from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1",Long.class)
                .setParameter("id",h.getId()).setParameter("prefix",PREFIX).getSingleResult()>0;
    }
    public static void restore(EntityManager em,HoaDon h) {
        if(!managed(em,h)) return;
        if(Integer.valueOf(6).equals(h.getTrangThai()) || Integer.valueOf(7).equals(h.getTrangThai()))
            throw AppException.conflict("Đơn đã được hủy/hoàn tiền");
        var rows=em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id=:id order by d.idSanPhamChiTiet.id",HoaDonChiTiet.class)
                .setParameter("id",h.getId()).getResultList();
        var locked=com.smashstep.datn.sales.service.TonKhoHoaDonCho.khoaBienThe(em,
                rows.stream().map(row->row.getIdSanPhamChiTiet().getId()).toList());
        for(var row:rows) {
            var v=locked.get(row.getIdSanPhamChiTiet().getId());
            if(v==null || row.getSoLuong()==null || row.getSoLuong()<1) throw AppException.conflict("Không thể hoàn kho của đơn hàng");
            try { v.setSoLuong(Math.addExact(Objects.requireNonNullElse(v.getSoLuong(),0),row.getSoLuong())); }
            catch(ArithmeticException e) { throw AppException.conflict("Tồn kho vượt giới hạn"); }
        }
        releaseVoucher(em,h);
    }
    public static void releaseVoucher(EntityManager em,HoaDon h) {
        if(!managed(em,h)) return;
        if(h.getIdPhieuGiamGia()!=null) {
            var voucher=em.find(PhieuGiamGia.class,h.getIdPhieuGiamGia().getId(),LockModeType.PESSIMISTIC_WRITE);
            voucher.setSoLuongDaDung(Math.max(0,Objects.requireNonNullElse(voucher.getSoLuongDaDung(),0)-1));
            var assigned=em.createQuery("select a from PhieuGiamGiaKhachHang a where a.idPhieuGiamGia.id=:v and a.idKhachHang.id=:c",PhieuGiamGiaKhachHang.class)
                    .setParameter("v",voucher.getId()).setParameter("c",h.getIdKhachHang().getId()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            for(var a:assigned) if(a.getNgaySuDung()!=null && a.getNgaySuDung().equals(h.getNgayTao())) a.setNgaySuDung(null);
        }
    }
    public static void collectCodOnCompletion(EntityManager em,HoaDon h) {
        if(!managed(em,h) || h.getNgayThanhToan()!=null || h.getIdPhuongThucThanhToan()==null
                || !"COD".equals(h.getIdPhuongThucThanhToan().getMaPhuongThuc())) return;
        if(em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.id=:id",Long.class)
                .setParameter("id",h.getId()).getSingleResult()!=0) throw AppException.conflict("Đơn COD đã có giao dịch; cần kiểm tra trước khi hoàn thành");
        var now=LocalDateTime.now(); var payment=new LichSuThanhToan();
        payment.setIdHoaDon(h); payment.setIdPhuongThucThanhToan(h.getIdPhuongThucThanhToan());
        payment.setSoTien(h.getThanhTien()); payment.setTrangThai(1); payment.setThoiGian(now);
        payment.setMoTa("Nhân viên xác nhận hoàn thành và đã thu COD"); em.persist(payment); h.setNgayThanhToan(now);
    }
}
