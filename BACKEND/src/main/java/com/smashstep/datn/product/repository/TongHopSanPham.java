package com.smashstep.datn.product.repository;

import java.math.BigDecimal;

public interface TongHopSanPham {
    Long getSanPhamId();
    Long getTongSoLuong();
    BigDecimal getGiaThapNhat();
    BigDecimal getGiaCaoNhat();
    Long getSoBienThe();
    Long getSoMau();
    Long getSoKichThuoc();
}
