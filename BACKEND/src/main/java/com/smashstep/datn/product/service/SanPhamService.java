package com.smashstep.datn.product.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SanPhamService {
    private final KhoaGhiSanPham khoaGhi;
    private final com.smashstep.datn.common.pricing.CurrentPriceService currentPrices;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HinhAnhSanPhamRepository hinhAnhRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final KieuDangRepository kieuDangRepository;
    private final CoGiayRepository coGiayRepository;
    private final XuatXuRepository xuatXuRepository;

    public PageResponse<SanPhamResponse> layDanhSachSanPham(int trang, int kichThuocTrang,
            String tuKhoa, Integer trangThai, Long danhMucId, Long thuongHieuId,
            Long chatLieuId, Long kieuDangId, Long coGiayId, Long xuatXuId) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        Specification<SanPham> boLoc = (bang, truyVan, tieuChi) -> {
            List<Predicate> dieuKien = new ArrayList<>();
            if (!QuyTacSanPham.boKhoangTrang(tuKhoa).isEmpty()) {
                String mauTimKiem = QuyTacSanPham.taoMauTimKiem(tuKhoa);
                dieuKien.add(tieuChi.or(
                        tieuChi.like(tieuChi.lower(bang.get("maSanPham")), mauTimKiem, '\\'),
                        tieuChi.like(tieuChi.lower(bang.get("tenSanPham")), mauTimKiem, '\\')));
            }
            if (trangThai != null) {
                dieuKien.add(tieuChi.equal(bang.get("trangThai"), trangThai));
            }
            if (danhMucId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idDanhMuc").get("id"), danhMucId));
            }
            if (thuongHieuId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idThuongHieu").get("id"), thuongHieuId));
            }
            if (chatLieuId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idChatLieu").get("id"), chatLieuId));
            }
            if (kieuDangId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idKieuDang").get("id"), kieuDangId));
            }
            if (coGiayId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idCoGiay").get("id"), coGiayId));
            }
            if (xuatXuId != null) {
                dieuKien.add(tieuChi.equal(bang.get("idXuatXu").get("id"), xuatXuId));
            }
            return tieuChi.and(dieuKien.toArray(Predicate[]::new));
        };
        Page<SanPham> ketQua = sanPhamRepository.findAll(boLoc,
                QuyTacSanPham.taoPhanTrang(trang, kichThuocTrang));
        List<Long> danhSachId = ketQua.getContent().stream().map(SanPham::getId).toList();
        Map<Long, TongHopSanPham> tongHop = layTongHop(danhSachId);
        Map<Long, String> anhChinh = HinhAnhSanPhamService.layAnhChinh(hinhAnhRepository, danhSachId);
        var variants = currentPrices.getProductVariants(danhSachId);
        var prices = currentPrices.getPrices(variants, LocalDateTime.now());
        var grouped = variants.stream().collect(java.util.stream.Collectors.groupingBy(v -> v.getIdSanPham().getId()));
        return PageResponse.from(ketQua.map(sanPham -> priceSummary(
                chuyenSangResponse(sanPham, tongHop.get(sanPham.getId()), anhChinh.get(sanPham.getId())),
                grouped.getOrDefault(sanPham.getId(), List.of()), prices)));
    }

    public SanPhamChiTietResponse layChiTietSanPham(Long id) {
        SanPham sanPham = timSanPham(id);
        TongHopSanPham tongHop = layTongHop(List.of(id)).get(id);
        String anhChinh = HinhAnhSanPhamService.layAnhChinh(hinhAnhRepository, List.of(id)).get(id);
        List<BienTheResponse> bienThe = new ArrayList<>();
        var variants = sanPhamChiTietRepository.findByIdSanPham_IdOrderByIdAsc(id);
        var prices = currentPrices.getPrices(variants, LocalDateTime.now());
        var variantImages = HinhAnhSanPhamService.layAnhChinhBienThe(hinhAnhRepository, variants);
        for (SanPhamChiTiet chiTiet : variants) {
            bienThe.add(SanPhamChiTietService.chuyenSangResponse(chiTiet, variantImages.get(chiTiet.getId()), prices.get(chiTiet.getId())));
        }
        List<HinhAnhSanPhamResponse> hinhAnh = new ArrayList<>();
        for (HinhAnhSanPham anh : hinhAnhRepository.findByIdSanPham_IdOrderByIdAsc(id)) {
            if (anh.getIdSanPhamChiTiet() == null) hinhAnh.add(HinhAnhSanPhamService.chuyenSangResponse(anh));
        }
        return new SanPhamChiTietResponse(priceSummary(chuyenSangResponse(sanPham, tongHop, anhChinh), variants, prices), bienThe, hinhAnh);
    }

    @Transactional
    public SanPhamResponse themSanPham(SanPhamThemRequest yeuCau) {
        khoaGhi.khoa("products");
        kiemTraTrungKhiLuu(SanPhamTrungRequest.from(yeuCau, null));
        String maSanPham = QuyTacSanPham.boKhoangTrang(yeuCau.getMaSanPham());
        if (sanPhamRepository.existsByMaSanPhamIgnoreCase(maSanPham)) {
            throw AppException.conflict("Mã sản phẩm đã tồn tại");
        }
        SanPham sanPham = new SanPham();
        sanPham.setMaSanPham(maSanPham);
        sanPham.setNgayTao(LocalDateTime.now());
        capNhatDuLieu(sanPham, yeuCau);
        return chuyenSangResponse(sanPhamRepository.save(sanPham), null);
    }

    @Transactional
    public SanPhamResponse suaSanPham(Long id, SanPhamSuaRequest yeuCau) {
        khoaGhi.khoa("products");
        SanPham sanPham = sanPhamRepository.timVaKhoaTheoId(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        if (yeuCau.getMaSanPham() != null
                && !Objects.equals(sanPham.getMaSanPham(), QuyTacSanPham.boKhoangTrang(yeuCau.getMaSanPham()))) {
            throw AppException.badRequest("Mã sản phẩm không được thay đổi khi sửa");
        }
        kiemTraTrungKhiLuu(SanPhamTrungRequest.from(yeuCau.chuyenSangThemRequest(), id));
        capNhatDuLieu(sanPham, yeuCau.chuyenSangThemRequest());
        sanPham.setNgayCapNhat(LocalDateTime.now());
        return chuyenSangResponse(sanPhamRepository.save(sanPham), layTongHop(List.of(id)).get(id));
    }

    @Transactional
    public SanPhamResponse doiTrangThai(Long id, Integer trangThai) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        SanPham sanPham = sanPhamRepository.timVaKhoaTheoId(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        sanPham.setTrangThai(trangThai);
        sanPham.setNgayCapNhat(LocalDateTime.now());
        return chuyenSangResponse(sanPhamRepository.save(sanPham), layTongHop(List.of(id)).get(id));
    }

    public KiemTraTrungResponse.Product kiemTraTrung(SanPhamTrungRequest request) {
        if (request.getExcludeId() != null) timSanPham(request.getExcludeId());
        var existing = timSanPhamTrung(request);
        return new KiemTraTrungResponse.Product(existing.isPresent(),
                existing.map(item -> chuyenSangResponse(item, null, null)).orElse(null));
    }

    private Optional<SanPham> timSanPhamTrung(SanPhamTrungRequest request) {
        Specification<SanPham> candidates = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("idDanhMuc").get("id"), request.getDanhMucId()));
            predicates.add(cb.equal(root.get("idThuongHieu").get("id"), request.getThuongHieuId()));
            predicates.add(cb.equal(root.get("idChatLieu").get("id"), request.getChatLieuId()));
            predicates.add(cb.equal(root.get("idKieuDang").get("id"), request.getKieuDangId()));
            predicates.add(cb.equal(root.get("idCoGiay").get("id"), request.getCoGiayId()));
            predicates.add(cb.equal(root.get("idXuatXu").get("id"), request.getXuatXuId()));
            predicates.add(cb.like(cb.lower(root.get("tenSanPham")),
                    QuyTacSanPham.businessNamePattern(request.getTenSanPham()), '\\'));
            if (request.getExcludeId() != null) predicates.add(cb.notEqual(root.get("id"), request.getExcludeId()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        String name = QuyTacSanPham.normalizeBusinessName(request.getTenSanPham());
        return sanPhamRepository.findAll(candidates).stream()
                .filter(item -> name.equals(QuyTacSanPham.normalizeBusinessName(item.getTenSanPham())))
                .min(Comparator.comparing(SanPham::getId));
    }

    private void kiemTraTrungKhiLuu(SanPhamTrungRequest request) {
        timSanPhamTrung(request).ifPresent(existing -> {
            throw AppException.conflict(request.getExcludeId() == null
                    ? "Sản phẩm với tên và bộ thuộc tính này đã tồn tại."
                    : "Đã tồn tại sản phẩm " + existing.getMaSanPham() + " có cùng tên và bộ thuộc tính.");
        });
    }

    private void capNhatDuLieu(SanPham sanPham, SanPhamThemRequest yeuCau) {
        DanhMuc danhMuc = danhMucRepository.findById(yeuCau.getDanhMucId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục"));
        ThuongHieu thuongHieu = thuongHieuRepository.findById(yeuCau.getThuongHieuId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thương hiệu"));
        ChatLieu chatLieu = chatLieuRepository.findById(yeuCau.getChatLieuId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy chất liệu"));
        KieuDang kieuDang = kieuDangRepository.findById(yeuCau.getKieuDangId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy kiểu dáng"));
        CoGiay coGiay = coGiayRepository.findById(yeuCau.getCoGiayId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy cổ giày"));
        XuatXu xuatXu = xuatXuRepository.findById(yeuCau.getXuatXuId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy xuất xứ"));
        QuyTacSanPham.kiemTraThuocTinhHoatDong(danhMuc.getTrangThai(),
                sanPham.getIdDanhMuc() == null ? null : sanPham.getIdDanhMuc().getId(), danhMuc.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(thuongHieu.getTrangThai(),
                sanPham.getIdThuongHieu() == null ? null : sanPham.getIdThuongHieu().getId(), thuongHieu.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(chatLieu.getTrangThai(),
                sanPham.getIdChatLieu() == null ? null : sanPham.getIdChatLieu().getId(), chatLieu.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(kieuDang.getTrangThai(),
                sanPham.getIdKieuDang() == null ? null : sanPham.getIdKieuDang().getId(), kieuDang.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(coGiay.getTrangThai(),
                sanPham.getIdCoGiay() == null ? null : sanPham.getIdCoGiay().getId(), coGiay.getId());
        QuyTacSanPham.kiemTraThuocTinhHoatDong(xuatXu.getTrangThai(),
                sanPham.getIdXuatXu() == null ? null : sanPham.getIdXuatXu().getId(), xuatXu.getId());
        sanPham.setIdDanhMuc(danhMuc);
        sanPham.setIdThuongHieu(thuongHieu);
        sanPham.setIdChatLieu(chatLieu);
        sanPham.setIdKieuDang(kieuDang);
        sanPham.setIdCoGiay(coGiay);
        sanPham.setIdXuatXu(xuatXu);
        sanPham.setTenSanPham(QuyTacSanPham.boKhoangTrang(yeuCau.getTenSanPham()));
        sanPham.setMoTaChiTiet(QuyTacSanPham.boKhoangTrang(yeuCau.getMoTaChiTiet()));
        sanPham.setTrangThai(yeuCau.getTrangThai());
    }

    private SanPham timSanPham(Long id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
    }

    private SanPhamResponse priceSummary(SanPhamResponse response, List<SanPhamChiTiet> variants,
            Map<Long, com.smashstep.datn.common.pricing.CurrentPrice> prices) {
        var values = variants.stream().map(v -> prices.get(v.getId())).filter(Objects::nonNull).toList();
        response.setGiaSauGiamThapNhat(values.stream().map(com.smashstep.datn.common.pricing.CurrentPrice::getEffectivePrice).min(java.math.BigDecimal::compareTo).orElse(response.getGiaThapNhat()));
        response.setGiaSauGiamCaoNhat(values.stream().map(com.smashstep.datn.common.pricing.CurrentPrice::getEffectivePrice).max(java.math.BigDecimal::compareTo).orElse(response.getGiaCaoNhat()));
        response.setDangGiamGia(values.stream().anyMatch(com.smashstep.datn.common.pricing.CurrentPrice::isDiscounted));
        response.setPhanTramGiamCaoNhat(values.stream().map(com.smashstep.datn.common.pricing.CurrentPrice::getDiscountPercent).max(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
        return response;
    }

    private Map<Long, TongHopSanPham> layTongHop(List<Long> danhSachId) {
        Map<Long, TongHopSanPham> ketQua = new LinkedHashMap<>();
        if (!danhSachId.isEmpty()) {
            for (TongHopSanPham tongHop : sanPhamChiTietRepository.tongHopTheoSanPham(danhSachId)) {
                ketQua.put(tongHop.getSanPhamId(), tongHop);
            }
        }
        return ketQua;
    }

    private SanPhamResponse chuyenSangResponse(SanPham sanPham, TongHopSanPham tongHop) {
        String anhChinh = HinhAnhSanPhamService.layAnhChinh(hinhAnhRepository,
                List.of(sanPham.getId())).get(sanPham.getId());
        return chuyenSangResponse(sanPham, tongHop, anhChinh);
    }

    private SanPhamResponse chuyenSangResponse(SanPham sanPham, TongHopSanPham tongHop, String anhChinh) {
        SanPhamResponse ketQua = new SanPhamResponse();
        ketQua.setId(sanPham.getId());
        ketQua.setMaSanPham(sanPham.getMaSanPham());
        ketQua.setTenSanPham(sanPham.getTenSanPham());
        if (sanPham.getIdDanhMuc() != null) {
            ketQua.setDanhMucId(sanPham.getIdDanhMuc().getId());
            ketQua.setTenDanhMuc(sanPham.getIdDanhMuc().getTenDanhMuc());
        }
        if (sanPham.getIdThuongHieu() != null) {
            ketQua.setThuongHieuId(sanPham.getIdThuongHieu().getId());
            ketQua.setTenThuongHieu(sanPham.getIdThuongHieu().getTenThuongHieu());
        }
        if (sanPham.getIdChatLieu() != null) {
            ketQua.setChatLieuId(sanPham.getIdChatLieu().getId());
            ketQua.setTenChatLieu(sanPham.getIdChatLieu().getTenChatLieu());
        }
        if (sanPham.getIdKieuDang() != null) {
            ketQua.setKieuDangId(sanPham.getIdKieuDang().getId());
            ketQua.setTenKieuDang(sanPham.getIdKieuDang().getTenKieuDang());
        }
        if (sanPham.getIdCoGiay() != null) {
            ketQua.setCoGiayId(sanPham.getIdCoGiay().getId());
            ketQua.setTenCoGiay(sanPham.getIdCoGiay().getTenCoGiay());
        }
        if (sanPham.getIdXuatXu() != null) {
            ketQua.setXuatXuId(sanPham.getIdXuatXu().getId());
            ketQua.setTenXuatXu(sanPham.getIdXuatXu().getTenXuatXu());
        }
        ketQua.setMoTaChiTiet(sanPham.getMoTaChiTiet());
        ketQua.setTrangThai(sanPham.getTrangThai());
        ketQua.setNgayTao(sanPham.getNgayTao());
        ketQua.setNgayCapNhat(sanPham.getNgayCapNhat());
        ketQua.setAnhChinh(anhChinh);
        if (tongHop != null) {
            ketQua.setTongSoLuong(Objects.requireNonNullElse(tongHop.getTongSoLuong(), 0L));
            ketQua.setGiaThapNhat(tongHop.getGiaThapNhat());
            ketQua.setGiaCaoNhat(tongHop.getGiaCaoNhat());
            ketQua.setTongSoBienThe(Objects.requireNonNullElse(tongHop.getSoBienThe(), 0L));
            ketQua.setSoMau(Objects.requireNonNullElse(tongHop.getSoMau(), 0L));
            ketQua.setSoKichThuoc(Objects.requireNonNullElse(tongHop.getSoKichThuoc(), 0L));
        }
        return ketQua;
    }
}
