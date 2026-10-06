package com.smashstep.datn.product.service;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThuocTinhSanPhamService {
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final CoGiayRepository coGiayRepository;
    private final KieuDangRepository kieuDangRepository;
    private final MauSacRepository mauSacRepository;
    private final KichThuocRepository kichThuocRepository;

    public Map<String, List<ThuocTinhResponse>> layLuaChon() {
        Map<String, List<ThuocTinhResponse>> ketQua = new LinkedHashMap<>();
        ketQua.put("categories", danhMucRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("brands", thuongHieuRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("materials", chatLieuRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("origins", xuatXuRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("collars", coGiayRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("styles", kieuDangRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("colors", mauSacRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        ketQua.put("sizes", kichThuocRepository.findAll(Sort.by("id")).stream().map(this::chuyenSangResponse).toList());
        return ketQua;
    }

    public PageResponse<ThuocTinhResponse> layDanhSach(String loai, int trang, int kichThuocTrang, String tuKhoa, Integer trangThai) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        var phanTrang = QuyTacSanPham.taoPhanTrang(trang, kichThuocTrang);
        return switch (loai) {
            case "categories" -> {
                var danhSach = danhMucRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maDanhMuc", "tenDanhMuc"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "brands" -> {
                var danhSach = thuongHieuRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maThuongHieu", "tenThuongHieu"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "materials" -> {
                var danhSach = chatLieuRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maChatLieu", "tenChatLieu"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "origins" -> {
                var danhSach = xuatXuRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maXuatXu", "tenXuatXu"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "collars" -> {
                var danhSach = coGiayRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maCoGiay", "tenCoGiay"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "styles" -> {
                var danhSach = kieuDangRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maKieuDang", "tenKieuDang"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "colors" -> {
                var danhSach = mauSacRepository.findAll(
                        timKiem(tuKhoa, trangThai, "maMauSac", "tenMauSac"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            case "sizes" -> {
                var danhSach = kichThuocRepository.findAll(
                        timKiem(tuKhoa, trangThai, null, "giaTri"), phanTrang);
                yield PageResponse.from(danhSach.map(this::chuyenSangResponse));
            }
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    private <T> Specification<T> timKiem(String tuKhoa, Integer trangThai, String truongMa, String truongTen) {
        return (bang, truyVan, tieuChi) -> {
            List<Predicate> dieuKien = new ArrayList<>();
            if (!QuyTacSanPham.boKhoangTrang(tuKhoa).isEmpty()) {
                String mauTimKiem = QuyTacSanPham.taoMauTimKiem(tuKhoa);
                Predicate dieuKienTen = tieuChi.like(tieuChi.lower(bang.get(truongTen)), mauTimKiem, '\\');
                dieuKien.add(truongMa == null ? dieuKienTen : tieuChi.or(dieuKienTen, tieuChi.like(tieuChi.lower(bang.get(truongMa)), mauTimKiem, '\\')));
            }
            if (trangThai != null) dieuKien.add(tieuChi.equal(bang.get("trangThai"), trangThai));
            return tieuChi.and(dieuKien.toArray(Predicate[]::new));
        };
    }

    public ThuocTinhResponse layChiTiet(String loai, Long id) {
        return switch (loai) {
            case "categories" -> chuyenSangResponse(danhMucRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục")));
            case "brands" -> chuyenSangResponse(thuongHieuRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy thương hiệu")));
            case "materials" -> chuyenSangResponse(chatLieuRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy chất liệu")));
            case "origins" -> chuyenSangResponse(xuatXuRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy xuất xứ")));
            case "collars" -> chuyenSangResponse(coGiayRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy cổ giày")));
            case "styles" -> chuyenSangResponse(kieuDangRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy kiểu dáng")));
            case "colors" -> chuyenSangResponse(mauSacRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy màu sắc")));
            case "sizes" -> chuyenSangResponse(kichThuocRepository.findById(id)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy kích thước")));
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    public String taoMaTiepTheo(String loai) {
        String tienTo = switch (loai) {
            case "categories" -> "DM";
            case "brands" -> "TH";
            case "materials" -> "CL";
            case "styles" -> "KD";
            case "collars" -> "CG";
            case "origins" -> "XX";
            case "colors" -> "MS";
            case "sizes" -> throw AppException.badRequest("Kích thước không có mã.");
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
        List<String> danhSachMa = switch (loai) {
            case "categories" -> danhMucRepository.layDanhSachMa();
            case "brands" -> thuongHieuRepository.layDanhSachMa();
            case "materials" -> chatLieuRepository.layDanhSachMa();
            case "styles" -> kieuDangRepository.layDanhSachMa();
            case "collars" -> coGiayRepository.layDanhSachMa();
            case "origins" -> xuatXuRepository.layDanhSachMa();
            case "colors" -> mauSacRepository.layDanhSachMa();
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
        BigInteger lonNhat = BigInteger.ZERO;
        for (String ma : danhSachMa) {
            if (ma != null && ma.matches(tienTo + "[0-9]+")) {
                lonNhat = lonNhat.max(new BigInteger(ma.substring(tienTo.length())));
            }
        }
        String soTiepTheo = lonNhat.add(BigInteger.ONE).toString();
        String maMoi = tienTo + "0".repeat(Math.max(0, 3 - soTiepTheo.length())) + soTiepTheo;
        if (maMoi.length() > 50) throw AppException.badRequest("Mã thuộc tính tiếp theo vượt quá 50 ký tự.");
        return maMoi;
    }

    private <T> Specification<T> kiemTraTrung(Long id, String truongMa, String truongTen, String ma, String ten) {
        return (bang, truyVan, tieuChi) -> {
            Predicate trungTen = tieuChi.equal(tieuChi.lower(bang.get(truongTen)),
                    QuyTacSanPham.boKhoangTrang(ten).toLowerCase(Locale.ROOT));
            Predicate trung = truongMa == null ? trungTen : tieuChi.or(trungTen,
                    tieuChi.equal(tieuChi.lower(bang.get(truongMa)),
                            QuyTacSanPham.boKhoangTrang(ma).toLowerCase(Locale.ROOT)));
            return tieuChi.and(tieuChi.notEqual(bang.get("id"), id == null ? 0L : id), trung);
        };
    }

    @Transactional
    public ThuocTinhResponse luuThuocTinh(String loai, Long id, ThuocTinhRequest yeuCau) {
        QuyTacSanPham.kiemTraTrangThai(yeuCau.getTrangThai());
        if ("sizes".equals(loai) && QuyTacSanPham.boKhoangTrang(yeuCau.getTen()).length() > 50)
            throw AppException.badRequest("Giá trị kích thước tối đa 50 ký tự");
        if ("colors".equals(loai) && !QuyTacSanPham.boKhoangTrang(yeuCau.getMaMauHex()).matches("^#[0-9a-fA-F]{6}$"))
            throw AppException.badRequest("Mã HEX phải có dạng #RRGGBB");
        String maMoi = id == null && !"sizes".equals(loai) ? taoMaTiepTheo(loai) : null;
        yeuCau = new ThuocTinhRequest(maMoi, yeuCau.getTen(), yeuCau.getGhiChu(),
                yeuCau.getMaMauHex(), yeuCau.getTrangThai());
        return switch (loai) {
            case "categories" -> luuDanhMuc(id, yeuCau);
            case "brands" -> luuThuongHieu(id, yeuCau);
            case "materials" -> luuChatLieu(id, yeuCau);
            case "origins" -> luuXuatXu(id, yeuCau);
            case "collars" -> luuCoGiay(id, yeuCau);
            case "styles" -> luuKieuDang(id, yeuCau);
            case "colors" -> luuMauSac(id, yeuCau);
            case "sizes" -> luuKichThuoc(id, yeuCau);
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    @Transactional
    public ThuocTinhResponse doiTrangThai(String loai, Long id, Integer trangThai) {
        QuyTacSanPham.kiemTraTrangThai(trangThai);
        switch (loai) {
            case "categories" -> {
                DanhMuc thuocTinh = danhMucRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(danhMucRepository.save(thuocTinh));
            }
            case "brands" -> {
                ThuongHieu thuocTinh = thuongHieuRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(thuongHieuRepository.save(thuocTinh));
            }
            case "materials" -> {
                ChatLieu thuocTinh = chatLieuRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(chatLieuRepository.save(thuocTinh));
            }
            case "origins" -> {
                XuatXu thuocTinh = xuatXuRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(xuatXuRepository.save(thuocTinh));
            }
            case "collars" -> {
                CoGiay thuocTinh = coGiayRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(coGiayRepository.save(thuocTinh));
            }
            case "styles" -> {
                KieuDang thuocTinh = kieuDangRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                return chuyenSangResponse(kieuDangRepository.save(thuocTinh));
            }
            case "colors" -> {
                MauSac thuocTinh = mauSacRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai);
                thuocTinh.setNgayCapNhat(LocalDateTime.now());
                return chuyenSangResponse(mauSacRepository.save(thuocTinh));
            }
            case "sizes" -> {
                KichThuoc thuocTinh = kichThuocRepository.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                thuocTinh.setTrangThai(trangThai); thuocTinh.setNgayCapNhat(LocalDateTime.now());
                return chuyenSangResponse(kichThuocRepository.save(thuocTinh));
            }
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        }
    }

    private ThuocTinhResponse luuDanhMuc(Long id, ThuocTinhRequest yeuCau) {
        DanhMuc thuocTinh = id == null ? new DanhMuc() : danhMucRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục"));
        if (id == null) thuocTinh.setMaDanhMuc(yeuCau.getMa());
        if (danhMucRepository.exists(kiemTraTrung(id, "maDanhMuc", "tenDanhMuc", thuocTinh.getMaDanhMuc(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên danh mục đã tồn tại");
        }
        thuocTinh.setTenDanhMuc(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setMoTa(QuyTacSanPham.boKhoangTrang(yeuCau.getGhiChu()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(danhMucRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(DanhMuc thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaDanhMuc(), thuocTinh.getTenDanhMuc(),
                thuocTinh.getMoTa(), null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuThuongHieu(Long id, ThuocTinhRequest yeuCau) {
        ThuongHieu thuocTinh = id == null ? new ThuongHieu() : thuongHieuRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thương hiệu"));
        if (id == null) thuocTinh.setMaThuongHieu(yeuCau.getMa());
        if (thuongHieuRepository.exists(kiemTraTrung(id, "maThuongHieu", "tenThuongHieu", thuocTinh.getMaThuongHieu(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên thương hiệu đã tồn tại");
        }
        thuocTinh.setTenThuongHieu(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(thuongHieuRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(ThuongHieu thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaThuongHieu(), thuocTinh.getTenThuongHieu(),
                null, null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuChatLieu(Long id, ThuocTinhRequest yeuCau) {
        ChatLieu thuocTinh = id == null ? new ChatLieu() : chatLieuRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy chất liệu"));
        if (id == null) thuocTinh.setMaChatLieu(yeuCau.getMa());
        if (chatLieuRepository.exists(kiemTraTrung(id, "maChatLieu", "tenChatLieu", thuocTinh.getMaChatLieu(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên chất liệu đã tồn tại");
        }
        thuocTinh.setTenChatLieu(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(chatLieuRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(ChatLieu thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaChatLieu(), thuocTinh.getTenChatLieu(),
                null, null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuXuatXu(Long id, ThuocTinhRequest yeuCau) {
        XuatXu thuocTinh = id == null ? new XuatXu() : xuatXuRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy xuất xứ"));
        if (id == null) thuocTinh.setMaXuatXu(yeuCau.getMa());
        if (xuatXuRepository.exists(kiemTraTrung(id, "maXuatXu", "tenXuatXu", thuocTinh.getMaXuatXu(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên xuất xứ đã tồn tại");
        }
        thuocTinh.setTenXuatXu(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(xuatXuRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(XuatXu thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaXuatXu(), thuocTinh.getTenXuatXu(),
                null, null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuCoGiay(Long id, ThuocTinhRequest yeuCau) {
        CoGiay thuocTinh = id == null ? new CoGiay() : coGiayRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy cổ giày"));
        if (id == null) thuocTinh.setMaCoGiay(yeuCau.getMa());
        if (coGiayRepository.exists(kiemTraTrung(id, "maCoGiay", "tenCoGiay", thuocTinh.getMaCoGiay(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên cổ giày đã tồn tại");
        }
        thuocTinh.setTenCoGiay(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(coGiayRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(CoGiay thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaCoGiay(), thuocTinh.getTenCoGiay(),
                null, null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuKieuDang(Long id, ThuocTinhRequest yeuCau) {
        KieuDang thuocTinh = id == null ? new KieuDang() : kieuDangRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy kiểu dáng"));
        if (id == null) thuocTinh.setMaKieuDang(yeuCau.getMa());
        if (kieuDangRepository.exists(kiemTraTrung(id, "maKieuDang", "tenKieuDang", thuocTinh.getMaKieuDang(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên kiểu dáng đã tồn tại");
        }
        thuocTinh.setTenKieuDang(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        return chuyenSangResponse(kieuDangRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(KieuDang thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaKieuDang(), thuocTinh.getTenKieuDang(),
                null, null, thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuMauSac(Long id, ThuocTinhRequest yeuCau) {
        MauSac thuocTinh = id == null ? new MauSac() : mauSacRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy màu sắc"));
        if (id == null) thuocTinh.setMaMauSac(yeuCau.getMa());
        if (mauSacRepository.exists(kiemTraTrung(id, "maMauSac", "tenMauSac", thuocTinh.getMaMauSac(), yeuCau.getTen()))) {
            throw AppException.conflict("Mã hoặc tên màu sắc đã tồn tại");
        }
        thuocTinh.setTenMauSac(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setMaMauHex(QuyTacSanPham.boKhoangTrang(yeuCau.getMaMauHex()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        if (id == null) {
            thuocTinh.setNgayTao(LocalDateTime.now());
        } else {
            thuocTinh.setNgayCapNhat(LocalDateTime.now());
        }
        return chuyenSangResponse(mauSacRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(MauSac thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), thuocTinh.getMaMauSac(), thuocTinh.getTenMauSac(),
                null, thuocTinh.getMaMauHex(), thuocTinh.getTrangThai());
    }

    private ThuocTinhResponse luuKichThuoc(Long id, ThuocTinhRequest yeuCau) {
        KichThuoc thuocTinh = id == null ? new KichThuoc() : kichThuocRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy kích thước"));
        if (kichThuocRepository.exists(kiemTraTrung(id, null, "giaTri", null, yeuCau.getTen()))) {
            throw AppException.conflict("Giá trị kích thước đã tồn tại");
        }
        thuocTinh.setGiaTri(QuyTacSanPham.boKhoangTrang(yeuCau.getTen()));
        thuocTinh.setGhiChu(QuyTacSanPham.boKhoangTrang(yeuCau.getGhiChu()));
        thuocTinh.setTrangThai(yeuCau.getTrangThai());
        if (id == null) thuocTinh.setNgayTao(LocalDateTime.now()); else thuocTinh.setNgayCapNhat(LocalDateTime.now());
        return chuyenSangResponse(kichThuocRepository.save(thuocTinh));
    }

    private ThuocTinhResponse chuyenSangResponse(KichThuoc thuocTinh) {
        return new ThuocTinhResponse(thuocTinh.getId(), null, thuocTinh.getGiaTri(),
                thuocTinh.getGhiChu(), null, thuocTinh.getTrangThai());
    }
}
