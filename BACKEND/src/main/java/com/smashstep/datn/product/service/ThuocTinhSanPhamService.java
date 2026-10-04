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
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThuocTinhSanPhamService {
    private final DanhMucRepository categories;
    private final ThuongHieuRepository brands;
    private final ChatLieuRepository materials;
    private final XuatXuRepository origins;
    private final CoGiayRepository collars;
    private final KieuDangRepository styles;
    private final MauSacRepository colors;
    private final KichThuocRepository sizes;

    public Map<String, List<ThuocTinhResponse>> options() {
        Map<String, List<ThuocTinhResponse>> result = new LinkedHashMap<>();
        result.put("categories", categories.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("brands", brands.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("materials", materials.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("origins", origins.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("collars", collars.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("styles", styles.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("colors", colors.findAll(Sort.by("id")).stream().map(this::response).toList());
        result.put("sizes", sizes.findAll(Sort.by("id")).stream().map(this::response).toList());
        return result;
    }

    public PageResponse<ThuocTinhResponse> list(String type, int page, int size, String keyword, Integer status) {
        QuyTacSanPham.status(status);
        var pageable = QuyTacSanPham.page(page, size);
        return switch (type) {
            case "categories" -> PageResponse.from(categories.findAll(search(keyword, status, "maDanhMuc", "tenDanhMuc"), pageable).map(this::response));
            case "brands" -> PageResponse.from(brands.findAll(search(keyword, status, "maThuongHieu", "tenThuongHieu"), pageable).map(this::response));
            case "materials" -> PageResponse.from(materials.findAll(search(keyword, status, "maChatLieu", "tenChatLieu"), pageable).map(this::response));
            case "origins" -> PageResponse.from(origins.findAll(search(keyword, status, "maXuatXu", "tenXuatXu"), pageable).map(this::response));
            case "collars" -> PageResponse.from(collars.findAll(search(keyword, status, "maCoGiay", "tenCoGiay"), pageable).map(this::response));
            case "styles" -> PageResponse.from(styles.findAll(search(keyword, status, "maKieuDang", "tenKieuDang"), pageable).map(this::response));
            case "colors" -> PageResponse.from(colors.findAll(search(keyword, status, "maMauSac", "tenMauSac"), pageable).map(this::response));
            case "sizes" -> PageResponse.from(sizes.findAll(search(keyword, status, null, "giaTri"), pageable).map(this::response));
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    private <T> Specification<T> search(String keyword, Integer status, String codeField, String nameField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!QuyTacSanPham.trim(keyword).isEmpty()) {
                String pattern = QuyTacSanPham.like(keyword);
                Predicate name = cb.like(cb.lower(root.get(nameField)), pattern, '\\');
                predicates.add(codeField == null ? name : cb.or(name, cb.like(cb.lower(root.get(codeField)), pattern, '\\')));
            }
            if (status != null) predicates.add(cb.equal(root.get("trangThai"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public ThuocTinhResponse detail(String type, Long id) {
        return switch (type) {
            case "categories" -> response(categories.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục")));
            case "brands" -> response(brands.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thương hiệu")));
            case "materials" -> response(materials.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy chất liệu")));
            case "origins" -> response(origins.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy xuất xứ")));
            case "collars" -> response(collars.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy cổ giày")));
            case "styles" -> response(styles.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy kiểu dáng")));
            case "colors" -> response(colors.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy màu sắc")));
            case "sizes" -> response(sizes.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy kích thước")));
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    private <T> Specification<T> duplicate(Long id, String codeField, String nameField, ThuocTinhRequest r) {
        return (root, query, cb) -> {
            Predicate name = cb.equal(cb.lower(root.get(nameField)), QuyTacSanPham.trim(r.getTen()).toLowerCase(Locale.ROOT));
            Predicate same = codeField == null ? name : cb.or(name,
                    cb.equal(cb.lower(root.get(codeField)), QuyTacSanPham.trim(r.getMa()).toLowerCase(Locale.ROOT)));
            return cb.and(cb.notEqual(root.get("id"), id == null ? 0L : id), same);
        };
    }

    @Transactional
    public ThuocTinhResponse save(String type, Long id, ThuocTinhRequest r) {
        QuyTacSanPham.status(r.getTrangThai());
        if (!"sizes".equals(type) && !QuyTacSanPham.trim(r.getMa()).matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$"))
            throw AppException.badRequest("Mã thuộc tính bắt buộc, tối đa 50 ký tự Latin/số, dấu chấm, gạch dưới hoặc gạch ngang");
        if ("sizes".equals(type) && QuyTacSanPham.trim(r.getTen()).length() > 50)
            throw AppException.badRequest("Giá trị kích thước tối đa 50 ký tự");
        if ("colors".equals(type) && !QuyTacSanPham.trim(r.getMaMauHex()).matches("^#[0-9a-fA-F]{6}$"))
            throw AppException.badRequest("Mã HEX phải có dạng #RRGGBB");
        return switch (type) {
            case "categories" -> saveDanhMuc(id, r);
            case "brands" -> saveThuongHieu(id, r);
            case "materials" -> saveChatLieu(id, r);
            case "origins" -> saveXuatXu(id, r);
            case "collars" -> saveCoGiay(id, r);
            case "styles" -> saveKieuDang(id, r);
            case "colors" -> saveMauSac(id, r);
            case "sizes" -> saveKichThuoc(id, r);
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    @Transactional
    public ThuocTinhResponse changeStatus(String type, Long id, Integer status) {
        QuyTacSanPham.status(status);
        switch (type) {
            case "categories" -> {
                DanhMuc item = categories.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(categories.save(item));
            }
            case "brands" -> {
                ThuongHieu item = brands.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(brands.save(item));
            }
            case "materials" -> {
                ChatLieu item = materials.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(materials.save(item));
            }
            case "origins" -> {
                XuatXu item = origins.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(origins.save(item));
            }
            case "collars" -> {
                CoGiay item = collars.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(collars.save(item));
            }
            case "styles" -> {
                KieuDang item = styles.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(styles.save(item));
            }
            case "colors" -> {
                MauSac item = colors.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status); item.setNgayCapNhat(LocalDateTime.now());
                return response(colors.save(item));
            }
            case "sizes" -> {
                KichThuoc item = sizes.findById(id).orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status); item.setNgayCapNhat(LocalDateTime.now());
                return response(sizes.save(item));
            }
            default -> throw AppException.badRequest("Loại thuộc tính không hợp lệ");
        }
    }

    private ThuocTinhResponse saveDanhMuc(Long id, ThuocTinhRequest r) {
        if (categories.exists(duplicate(id, "maDanhMuc", "tenDanhMuc", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        DanhMuc item = id == null ? new DanhMuc() : categories.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaDanhMuc(QuyTacSanPham.trim(r.getMa()));
        item.setTenDanhMuc(QuyTacSanPham.trim(r.getTen()));
        item.setMoTa(QuyTacSanPham.trim(r.getGhiChu()));
        item.setTrangThai(r.getTrangThai());
        return response(categories.save(item));
    }

    private ThuocTinhResponse response(DanhMuc item) {
        return new ThuocTinhResponse(item.getId(), item.getMaDanhMuc(), item.getTenDanhMuc(),
                item.getMoTa(), null, item.getTrangThai());
    }

    private ThuocTinhResponse saveThuongHieu(Long id, ThuocTinhRequest r) {
        if (brands.exists(duplicate(id, "maThuongHieu", "tenThuongHieu", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        ThuongHieu item = id == null ? new ThuongHieu() : brands.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaThuongHieu(QuyTacSanPham.trim(r.getMa()));
        item.setTenThuongHieu(QuyTacSanPham.trim(r.getTen()));
        item.setTrangThai(r.getTrangThai());
        return response(brands.save(item));
    }

    private ThuocTinhResponse response(ThuongHieu item) {
        return new ThuocTinhResponse(item.getId(), item.getMaThuongHieu(), item.getTenThuongHieu(),
                null, null, item.getTrangThai());
    }

    private ThuocTinhResponse saveChatLieu(Long id, ThuocTinhRequest r) {
        if (materials.exists(duplicate(id, "maChatLieu", "tenChatLieu", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        ChatLieu item = id == null ? new ChatLieu() : materials.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaChatLieu(QuyTacSanPham.trim(r.getMa()));
        item.setTenChatLieu(QuyTacSanPham.trim(r.getTen()));
        item.setTrangThai(r.getTrangThai());
        return response(materials.save(item));
    }

    private ThuocTinhResponse response(ChatLieu item) {
        return new ThuocTinhResponse(item.getId(), item.getMaChatLieu(), item.getTenChatLieu(),
                null, null, item.getTrangThai());
    }

    private ThuocTinhResponse saveXuatXu(Long id, ThuocTinhRequest r) {
        if (origins.exists(duplicate(id, "maXuatXu", "tenXuatXu", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        XuatXu item = id == null ? new XuatXu() : origins.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaXuatXu(QuyTacSanPham.trim(r.getMa()));
        item.setTenXuatXu(QuyTacSanPham.trim(r.getTen()));
        item.setTrangThai(r.getTrangThai());
        return response(origins.save(item));
    }

    private ThuocTinhResponse response(XuatXu item) {
        return new ThuocTinhResponse(item.getId(), item.getMaXuatXu(), item.getTenXuatXu(),
                null, null, item.getTrangThai());
    }

    private ThuocTinhResponse saveCoGiay(Long id, ThuocTinhRequest r) {
        if (collars.exists(duplicate(id, "maCoGiay", "tenCoGiay", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        CoGiay item = id == null ? new CoGiay() : collars.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaCoGiay(QuyTacSanPham.trim(r.getMa()));
        item.setTenCoGiay(QuyTacSanPham.trim(r.getTen()));
        item.setTrangThai(r.getTrangThai());
        return response(collars.save(item));
    }

    private ThuocTinhResponse response(CoGiay item) {
        return new ThuocTinhResponse(item.getId(), item.getMaCoGiay(), item.getTenCoGiay(),
                null, null, item.getTrangThai());
    }

    private ThuocTinhResponse saveKieuDang(Long id, ThuocTinhRequest r) {
        if (styles.exists(duplicate(id, "maKieuDang", "tenKieuDang", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        KieuDang item = id == null ? new KieuDang() : styles.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaKieuDang(QuyTacSanPham.trim(r.getMa()));
        item.setTenKieuDang(QuyTacSanPham.trim(r.getTen()));
        item.setTrangThai(r.getTrangThai());
        return response(styles.save(item));
    }

    private ThuocTinhResponse response(KieuDang item) {
        return new ThuocTinhResponse(item.getId(), item.getMaKieuDang(), item.getTenKieuDang(),
                null, null, item.getTrangThai());
    }

    private ThuocTinhResponse saveMauSac(Long id, ThuocTinhRequest r) {
        if (colors.exists(duplicate(id, "maMauSac", "tenMauSac", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        MauSac item = id == null ? new MauSac() : colors.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setMaMauSac(QuyTacSanPham.trim(r.getMa()));
        item.setTenMauSac(QuyTacSanPham.trim(r.getTen()));
        item.setMaMauHex(QuyTacSanPham.trim(r.getMaMauHex()));
        item.setTrangThai(r.getTrangThai());
        if (id == null) item.setNgayTao(LocalDateTime.now()); else item.setNgayCapNhat(LocalDateTime.now());
        return response(colors.save(item));
    }

    private ThuocTinhResponse response(MauSac item) {
        return new ThuocTinhResponse(item.getId(), item.getMaMauSac(), item.getTenMauSac(),
                null, item.getMaMauHex(), item.getTrangThai());
    }

    private ThuocTinhResponse saveKichThuoc(Long id, ThuocTinhRequest r) {
        if (sizes.exists(duplicate(id, null, "giaTri", r)))
            throw AppException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        KichThuoc item = id == null ? new KichThuoc() : sizes.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thuộc tính"));
        item.setGiaTri(QuyTacSanPham.trim(r.getTen()));
        item.setGhiChu(QuyTacSanPham.trim(r.getGhiChu()));
        item.setTrangThai(r.getTrangThai());
        if (id == null) item.setNgayTao(LocalDateTime.now()); else item.setNgayCapNhat(LocalDateTime.now());
        return response(sizes.save(item));
    }

    private ThuocTinhResponse response(KichThuoc item) {
        return new ThuocTinhResponse(item.getId(), null, item.getGiaTri(),
                item.getGhiChu(), null, item.getTrangThai());
    }
}
