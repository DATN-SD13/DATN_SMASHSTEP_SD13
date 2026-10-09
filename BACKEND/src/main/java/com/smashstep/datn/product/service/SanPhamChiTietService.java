package com.smashstep.datn.product.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SanPhamChiTietService {
    private final com.smashstep.datn.common.pricing.CurrentPriceService currentPrices;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;
    private final HinhAnhSanPhamRepository hinhAnhRepository;

    public PageResponse<BienTheResponse> layDanhSach(int trang, int kichThuocTrang,
            String tuKhoa, Integer trangThai, Long sanPhamId, Long mauSacId, Long kichThuocId) {
        return layDanhSach(trang, kichThuocTrang, tuKhoa, trangThai, sanPhamId, mauSacId, kichThuocId, null);
    }

    public PageResponse<BienTheResponse> layDanhSach(int trang, int kichThuocTrang,
            String tuKhoa, Integer trangThai, Long sanPhamId, Long mauSacId, Long kichThuocId, Boolean kichHoat) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        Specification<SanPhamChiTiet> boLoc = (bang, truyVan, tieuChi) -> {
            List<Predicate> dieuKien = new ArrayList<>();
            if (!QuyTacSanPham.boKhoangTrang(tuKhoa).isEmpty()) {
                String mauTimKiem = QuyTacSanPham.taoMauTimKiem(tuKhoa);
                dieuKien.add(tieuChi.or(
                        tieuChi.like(tieuChi.lower(bang.get("sku")), mauTimKiem, '\\'),
                        tieuChi.like(tieuChi.lower(bang.get("maChiTietSanPham")), mauTimKiem, '\\'),
                        tieuChi.like(tieuChi.lower(bang.get("idSanPham").get("maSanPham")), mauTimKiem, '\\'),
                        tieuChi.like(tieuChi.lower(bang.get("idSanPham").get("tenSanPham")), mauTimKiem, '\\')));
            }
            if (trangThai != null) {
                dieuKien.add(tieuChi.equal(bang.get("trangThai"), trangThai));
            }
            if (kichHoat != null) {
                dieuKien.add(tieuChi.equal(bang.get("kichHoat"), kichHoat));
            }
            if (sanPhamId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idSanPham").get("id"), sanPhamId));
            }
            if (mauSacId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idMauSac").get("id"), mauSacId));
            }
            if (kichThuocId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idKichThuoc").get("id"), kichThuocId));
            }
            return tieuChi.and(dieuKien.toArray(Predicate[]::new));
        };
        var ketQua = sanPhamChiTietRepository.findAll(boLoc,
                QuyTacSanPham.taoPhanTrang(trang, kichThuocTrang));
        Map<Long, String> anhChinh = HinhAnhSanPhamService.layAnhChinhBienThe(hinhAnhRepository, ketQua.getContent());
        var prices = currentPrices.getPrices(ketQua.getContent(), LocalDateTime.now());
        return PageResponse.from(ketQua.map(chiTiet ->
                chuyenSangResponse(chiTiet, anhChinh.get(chiTiet.getId()), prices.get(chiTiet.getId()))));
    }

    public List<BienTheResponse> layTheoSanPham(Long sanPhamId) {
        sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        List<BienTheResponse> ketQua = new ArrayList<>();
        var rows = sanPhamChiTietRepository.findByIdSanPham_IdOrderByIdAsc(sanPhamId);
        var anhChinh = HinhAnhSanPhamService.layAnhChinhBienThe(hinhAnhRepository, rows);
        var prices = currentPrices.getPrices(rows, LocalDateTime.now());
        for (SanPhamChiTiet chiTiet : rows) {
            ketQua.add(chuyenSangResponse(chiTiet, anhChinh.get(chiTiet.getId()), prices.get(chiTiet.getId())));
        }
        return ketQua;
    }

    public BienTheResponse layChiTiet(Long id) {
        SanPhamChiTiet chiTiet = timChiTiet(id);
        Long sanPhamId = chiTiet.getIdSanPham().getId();
        String anhChinh = HinhAnhSanPhamService.layAnhChinhBienThe(hinhAnhRepository, List.of(chiTiet)).get(id);
        return chuyenSangResponse(chiTiet, anhChinh, currentPrices.getPrice(chiTiet, LocalDateTime.now()));
    }

    @Transactional
    public BienTheResponse themSanPhamChiTiet(SanPhamChiTietThemRequest yeuCau) {
        if (yeuCau.getSanPhamId() == null) {
            throw AppException.badRequest("ID sản phẩm không được trống");
        }
        return themSanPhamChiTiet(yeuCau.getSanPhamId(), yeuCau);
    }

    @Transactional
    public BienTheResponse themSanPhamChiTiet(Long sanPhamId, SanPhamChiTietThemRequest yeuCau) {
        if (yeuCau.getSanPhamId() != null && !sanPhamId.equals(yeuCau.getSanPhamId())) {
            throw AppException.badRequest("Biến thể phải thuộc sản phẩm đang chọn");
        }
        yeuCau.setSanPhamId(sanPhamId);
        return luuBienTheMoi(khoaSanPham(sanPhamId), yeuCau);
    }

    @Transactional
    public List<BienTheResponse> themDanhSachBienThe(Long sanPhamId, List<SanPhamChiTietThemRequest> danhSach) {
        SanPham sanPham = khoaSanPham(sanPhamId);
        Set<String> danhSachMa = new HashSet<>();
        Set<String> danhSachSku = new HashSet<>();
        Set<String> danhSachMauVaSize = new HashSet<>();
        for (SanPhamChiTietThemRequest yeuCau : danhSach) {
            if (yeuCau.getSanPhamId() != null && !sanPhamId.equals(yeuCau.getSanPhamId())) {
                throw AppException.badRequest("Biến thể phải thuộc sản phẩm đang chọn");
            }
            yeuCau.setSanPhamId(sanPhamId);
            String ma = QuyTacSanPham.boKhoangTrang(yeuCau.getMaChiTietSanPham()).toLowerCase(Locale.ROOT);
            String sku = QuyTacSanPham.boKhoangTrang(yeuCau.getSku()).toLowerCase(Locale.ROOT);
            if (!danhSachMa.add(ma)) {
                throw AppException.conflict("Mã chi tiết sản phẩm trùng trong danh sách tạo");
            }
            if (!danhSachSku.add(sku)) {
                throw AppException.conflict("SKU trùng trong danh sách tạo");
            }
            if (!danhSachMauVaSize.add(yeuCau.getMauSacId() + ":" + yeuCau.getKichThuocId())) {
                throw AppException.conflict("Biến thể màu và kích thước trùng trong danh sách tạo");
            }
        }
        List<BienTheResponse> ketQua = new ArrayList<>();
        for (SanPhamChiTietThemRequest yeuCau : danhSach) {
            ketQua.add(luuBienTheMoi(sanPham, yeuCau));
        }
        return ketQua;
    }

    private BienTheResponse luuBienTheMoi(SanPham sanPham, SanPhamChiTietThemRequest yeuCau) {
        SanPhamChiTiet chiTiet = new SanPhamChiTiet();
        chiTiet.setIdSanPham(sanPham);
        chiTiet.setNgayTao(LocalDateTime.now());
        capNhatDuLieu(chiTiet, yeuCau);
        return chuyenSangResponse(sanPhamChiTietRepository.save(chiTiet), null);
    }

    @Transactional
    public BienTheResponse suaSanPhamChiTiet(Long id, SanPhamChiTietSuaRequest yeuCauSua) {
        SanPhamChiTietThemRequest yeuCau = yeuCauSua.chuyenSangThemRequest();
        SanPhamChiTiet chiTiet = timChiTiet(id);
        if (yeuCau.getSanPhamId() == null) {
            yeuCau.setSanPhamId(chiTiet.getIdSanPham().getId());
        }
        if (yeuCau.getMaChiTietSanPham() == null) {
            yeuCau.setMaChiTietSanPham(chiTiet.getMaChiTietSanPham());
        }
        if (!chiTiet.getIdSanPham().getId().equals(yeuCau.getSanPhamId())) {
            throw AppException.badRequest("Không thể chuyển biến thể sang sản phẩm khác");
        }
        khoaSanPham(yeuCau.getSanPhamId());
        capNhatDuLieu(chiTiet, yeuCau);
        chiTiet.setNgayCapNhat(LocalDateTime.now());
        return chuyenSangResponse(sanPhamChiTietRepository.save(chiTiet), null);
    }

    @Transactional
    public BienTheResponse doiTrangThai(Long id, Integer trangThai) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        SanPhamChiTiet chiTiet = timChiTiet(id);
        khoaSanPham(chiTiet.getIdSanPham().getId());
        chiTiet.setTrangThai(trangThai);
        chiTiet.setNgayCapNhat(LocalDateTime.now());
        return chuyenSangResponse(sanPhamChiTietRepository.save(chiTiet), null);
    }

    private void capNhatDuLieu(SanPhamChiTiet chiTiet, SanPhamChiTietThemRequest yeuCau) {
        Long id = chiTiet.getId() == null ? 0L : chiTiet.getId();
        String ma = QuyTacSanPham.boKhoangTrang(yeuCau.getMaChiTietSanPham());
        String sku = QuyTacSanPham.boKhoangTrang(yeuCau.getSku());
        if (sanPhamChiTietRepository.existsByMaChiTietSanPhamIgnoreCaseAndIdNot(ma, id)) {
            throw AppException.conflict("Mã chi tiết sản phẩm đã tồn tại");
        }
        if (sanPhamChiTietRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw AppException.conflict("SKU đã tồn tại");
        }
        // Không cho trùng màu và size trong cùng sản phẩm.
        if (sanPhamChiTietRepository.existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_IdAndIdNot(
                yeuCau.getSanPhamId(), yeuCau.getMauSacId(), yeuCau.getKichThuocId(), id)) {
            throw AppException.conflict("Biến thể màu và kích thước này đã tồn tại");
        }
        MauSac mauSac = mauSacRepository.findById(yeuCau.getMauSacId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy màu sắc"));
        KichThuoc kichThuoc = kichThuocRepository.findById(yeuCau.getKichThuocId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy kích thước"));
        QuyTacSanPham.kiemTraThuocTinhHoatDong(mauSac.getTrangThai(),
                chiTiet.getIdMauSac() == null ? null : chiTiet.getIdMauSac().getId(), mauSac.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(kichThuoc.getTrangThai(),
                chiTiet.getIdKichThuoc() == null ? null : chiTiet.getIdKichThuoc().getId(), kichThuoc.getId());
        chiTiet.setMaChiTietSanPham(ma);
        chiTiet.setSku(sku);
        chiTiet.setIdMauSac(mauSac);
        chiTiet.setIdKichThuoc(kichThuoc);
        chiTiet.setSoLuong(yeuCau.getSoLuong());
        chiTiet.setGiaBan(yeuCau.getGiaBan());
        chiTiet.setKichHoat(yeuCau.getKichHoat());
        chiTiet.setTrangThai(yeuCau.getTrangThai());
    }

    private SanPham khoaSanPham(Long id) {
        return sanPhamRepository.timVaKhoaTheoId(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
    }

    private SanPhamChiTiet timChiTiet(Long id) {
        return sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy biến thể"));
    }

    static BienTheResponse chuyenSangResponse(SanPhamChiTiet variant, String image, com.smashstep.datn.common.pricing.CurrentPrice price) {
        BienTheResponse response = chuyenSangResponse(variant, image);
        response.setGiaSauGiam(price.getEffectivePrice()); response.setPhanTramGiamHienTai(price.getDiscountPercent());
        response.setDangGiamGia(price.isDiscounted()); response.setMaDotGiamGia(price.getCampaignCode()); response.setTenDotGiamGia(price.getCampaignName());
        return response;
    }

    static BienTheResponse chuyenSangResponse(SanPhamChiTiet chiTiet, String anhChinh) {
        BienTheResponse ketQua = new BienTheResponse();
        ketQua.setId(chiTiet.getId());
        ketQua.setSanPhamId(chiTiet.getIdSanPham().getId());
        ketQua.setMaSanPham(chiTiet.getIdSanPham().getMaSanPham());
        ketQua.setTenSanPham(chiTiet.getIdSanPham().getTenSanPham());
        ketQua.setMaChiTietSanPham(chiTiet.getMaChiTietSanPham());
        ketQua.setSku(chiTiet.getSku());
        if (chiTiet.getIdMauSac() != null) {
            ketQua.setMauSacId(chiTiet.getIdMauSac().getId());
            ketQua.setTenMauSac(chiTiet.getIdMauSac().getTenMauSac());
            ketQua.setMaMauSac(chiTiet.getIdMauSac().getMaMauSac());
            ketQua.setMaMauHex(chiTiet.getIdMauSac().getMaMauHex());
        }
        if (chiTiet.getIdKichThuoc() != null) {
            ketQua.setKichThuocId(chiTiet.getIdKichThuoc().getId());
            ketQua.setGiaTriKichThuoc(chiTiet.getIdKichThuoc().getGiaTri());
        }
        ketQua.setSoLuong(chiTiet.getSoLuong());
        ketQua.setGiaBan(chiTiet.getGiaBan());
        ketQua.setKichHoat(chiTiet.getKichHoat());
        ketQua.setTrangThai(chiTiet.getTrangThai());
        ketQua.setNgayTao(chiTiet.getNgayTao());
        ketQua.setNgayCapNhat(chiTiet.getNgayCapNhat());
        ketQua.setAnhChinh(anhChinh);
        return ketQua;
    }
}
