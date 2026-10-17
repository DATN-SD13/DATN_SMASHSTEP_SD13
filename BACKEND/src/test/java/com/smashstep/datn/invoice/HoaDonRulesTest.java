package com.smashstep.datn.invoice;

import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.invoice.dto.HoaDonDetailDto;
import com.smashstep.datn.invoice.dto.HoaDonListDto;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.enums.HinhThucNhan;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import com.smashstep.datn.invoice.rules.HoaDonRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HoaDonRulesTest {
    @ParameterizedTest
    @CsvSource({"0,false,0", "0,true,1", "1,false,1", "1,true,1", "2,false,1", "2,true,1"})
    void canonicalTypeAndCustomerDetermineIdenticalListAndDetailResponses(int type, boolean customer, int expected) {
        var invoice = new HoaDon(); invoice.setLoaiHoaDon(type); invoice.setTrangThai(1);
        if (customer) invoice.setIdKhachHang(new KhachHang());
        assertEquals(expected, HoaDonRules.resolveHinhThucNhan(invoice).getMa());
        var list = HoaDonListDto.from(invoice); var detail = HoaDonDetailDto.from(invoice, List.of());
        assertEquals(expected, list.getMaHinhThucNhan()); assertEquals(expected, detail.getMaHinhThucNhan());
        assertEquals(list.getHinhThucNhan(), detail.getHinhThucNhan());
        assertEquals(expected == 0 ? "Nhận tại quầy" : "Giao hàng", detail.getHinhThucNhan());
        assertEquals(expected == 0 ? 5 : 2, detail.getMaTrangThaiTiepTheo());
    }
    @Test void nullAndUnknownLegacyDataAreNullSafeAndUseAddressOrCarrier() {
        assertEquals(HinhThucNhan.NHAN_TAI_QUAY, HoaDonRules.resolveHinhThucNhan(null));
        var invoice = new HoaDon();
        assertEquals(HinhThucNhan.NHAN_TAI_QUAY, HoaDonRules.resolveHinhThucNhan(invoice));
        invoice.setDiaChiGiaoHang("Hà Nội");
        assertEquals(HinhThucNhan.GIAO_HANG, HoaDonRules.resolveHinhThucNhan(invoice));
        invoice.setLoaiHoaDon(99); invoice.setDiaChiGiaoHang(" "); invoice.setDonViVanChuyen("GHN");
        assertEquals(HinhThucNhan.GIAO_HANG, HoaDonRules.resolveHinhThucNhan(invoice));
        invoice.setDonViVanChuyen("  ");
        assertEquals(HinhThucNhan.NHAN_TAI_QUAY, HoaDonRules.resolveHinhThucNhan(invoice));
    }
    @Test void pickupSkipsDeliveryStagesAndDeliveryKeepsEveryStage() {
        assertEquals(TrangThaiHoaDon.HOAN_THANH, TrangThaiHoaDon.trangThaiTiepTheo(1,false));
        assertEquals(TrangThaiHoaDon.CHO_GIAO_HANG, TrangThaiHoaDon.trangThaiTiepTheo(1,true));
        assertEquals(TrangThaiHoaDon.DANG_GIAO_HANG, TrangThaiHoaDon.trangThaiTiepTheo(2,true));
        assertEquals(TrangThaiHoaDon.DA_GIAO_HANG, TrangThaiHoaDon.trangThaiTiepTheo(3,true));
        assertEquals(TrangThaiHoaDon.HOAN_THANH, TrangThaiHoaDon.trangThaiTiepTheo(4,true));
        assertEquals(TrangThaiHoaDon.HOAN_THANH, TrangThaiHoaDon.trangThaiTiepTheo(8,false));
    }
}
