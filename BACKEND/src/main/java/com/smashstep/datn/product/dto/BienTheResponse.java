package com.smashstep.datn.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BienTheResponse {
    private Long id;
    private Long sanPhamId;
    private String maSanPham;
    private String tenSanPham;
    private String maChiTietSanPham;
    private String sku;
    private Long mauSacId;
    private String tenMauSac;
    private String maMauHex;
    private Long kichThuocId;
    private String giaTriKichThuoc;
    private Integer soLuong;
    private BigDecimal giaBan;
    private Boolean kichHoat;
    private Integer trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String anhChinh;
    private String maMauSac;

    private BigDecimal giaSauGiam;
    private BigDecimal phanTramGiamHienTai;
    private Boolean dangGiamGia;
    private String maDotGiamGia;
    private String tenDotGiamGia;

    public Long getIdSanPham() { return sanPhamId; }
    public Long getIdMauSac() { return mauSacId; }
    public Long getIdKichThuoc() { return kichThuocId; }
    public Long getProductId() { return sanPhamId; }
}
