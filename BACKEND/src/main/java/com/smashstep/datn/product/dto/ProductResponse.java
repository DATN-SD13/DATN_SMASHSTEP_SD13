package com.smashstep.datn.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private Long danhMucId;
    private String tenDanhMuc;
    private Long thuongHieuId;
    private String tenThuongHieu;
    private Long chatLieuId;
    private String tenChatLieu;
    private Long kieuDangId;
    private String tenKieuDang;
    private Long coGiayId;
    private String tenCoGiay;
    private Long xuatXuId;
    private String tenXuatXu;
    private String moTaChiTiet;
    private Integer trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private long tongSoLuong;
    private BigDecimal giaThapNhat;
    private BigDecimal giaCaoNhat;
    private long tongSoBienThe;
    private long soMau;
    private long soKichThuoc;
    private String anhChinh;

    public String getDanhMuc() { return tenDanhMuc; }
    public String getThuongHieu() { return tenThuongHieu; }
    public String getChatLieu() { return tenChatLieu; }
    public String getKieuDang() { return tenKieuDang; }
    public String getCoGiay() { return tenCoGiay; }
    public String getXuatXu() { return tenXuatXu; }
    public long getSoBienThe() { return tongSoBienThe; }
}
