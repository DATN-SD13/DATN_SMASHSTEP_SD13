package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.ProductDtos.*;
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
public class ProductAttributeService {
    private final DanhMucRepository categories;
    private final ThuongHieuRepository brands;
    private final ChatLieuRepository materials;
    private final XuatXuRepository origins;
    private final CoGiayRepository collars;
    private final KieuDangRepository styles;
    private final MauSacRepository colors;
    private final KichThuocRepository sizes;

    public Map<String, List<AttributeResponse>> options() {
        Map<String, List<AttributeResponse>> result = new LinkedHashMap<>();
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

    public PageResponse<AttributeResponse> list(String type, int page, int size, String keyword, Integer status) {
        ProductRules.status(status);
        var pageable = ProductRules.page(page, size);
        return switch (type) {
            case "categories" -> PageResponse.from(categories.findAll(search(keyword, status, "maDanhMuc", "tenDanhMuc"), pageable).map(this::response));
            case "brands" -> PageResponse.from(brands.findAll(search(keyword, status, "maThuongHieu", "tenThuongHieu"), pageable).map(this::response));
            case "materials" -> PageResponse.from(materials.findAll(search(keyword, status, "maChatLieu", "tenChatLieu"), pageable).map(this::response));
            case "origins" -> PageResponse.from(origins.findAll(search(keyword, status, "maXuatXu", "tenXuatXu"), pageable).map(this::response));
            case "collars" -> PageResponse.from(collars.findAll(search(keyword, status, "maCoGiay", "tenCoGiay"), pageable).map(this::response));
            case "styles" -> PageResponse.from(styles.findAll(search(keyword, status, "maKieuDang", "tenKieuDang"), pageable).map(this::response));
            case "colors" -> PageResponse.from(colors.findAll(search(keyword, status, "maMauSac", "tenMauSac"), pageable).map(this::response));
            case "sizes" -> PageResponse.from(sizes.findAll(search(keyword, status, null, "giaTri"), pageable).map(this::response));
            default -> throw ProductException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    private <T> Specification<T> search(String keyword, Integer status, String codeField, String nameField) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ProductRules.trim(keyword).isEmpty()) {
                String pattern = ProductRules.like(keyword);
                Predicate name = cb.like(cb.lower(root.get(nameField)), pattern, '\\');
                predicates.add(codeField == null ? name : cb.or(name, cb.like(cb.lower(root.get(codeField)), pattern, '\\')));
            }
            if (status != null) predicates.add(cb.equal(root.get("trangThai"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private <T> Specification<T> duplicate(Long id, String codeField, String nameField, AttributeRequest r) {
        return (root, query, cb) -> {
            Predicate name = cb.equal(cb.lower(root.get(nameField)), ProductRules.trim(r.ten()).toLowerCase(Locale.ROOT));
            Predicate same = codeField == null ? name : cb.or(name,
                    cb.equal(cb.lower(root.get(codeField)), ProductRules.trim(r.ma()).toLowerCase(Locale.ROOT)));
            return cb.and(cb.notEqual(root.get("id"), id == null ? 0L : id), same);
        };
    }

    @Transactional
    public AttributeResponse save(String type, Long id, AttributeRequest r) {
        ProductRules.status(r.trangThai());
        if (!"sizes".equals(type) && !ProductRules.trim(r.ma()).matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$"))
            throw ProductException.badRequest("Mã thuộc tính bắt buộc, tối đa 50 ký tự Latin/số, dấu chấm, gạch dưới hoặc gạch ngang");
        if ("sizes".equals(type) && ProductRules.trim(r.ten()).length() > 50)
            throw ProductException.badRequest("Giá trị kích thước tối đa 50 ký tự");
        if ("colors".equals(type) && !ProductRules.trim(r.maMauHex()).matches("^#[0-9a-fA-F]{6}$"))
            throw ProductException.badRequest("Mã HEX phải có dạng #RRGGBB");
        return switch (type) {
            case "categories" -> saveDanhMuc(id, r);
            case "brands" -> saveThuongHieu(id, r);
            case "materials" -> saveChatLieu(id, r);
            case "origins" -> saveXuatXu(id, r);
            case "collars" -> saveCoGiay(id, r);
            case "styles" -> saveKieuDang(id, r);
            case "colors" -> saveMauSac(id, r);
            case "sizes" -> saveKichThuoc(id, r);
            default -> throw ProductException.badRequest("Loại thuộc tính không hợp lệ");
        };
    }

    @Transactional
    public AttributeResponse changeStatus(String type, Long id, Integer status) {
        ProductRules.status(status);
        switch (type) {
            case "categories" -> {
                DanhMuc item = categories.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(categories.save(item));
            }
            case "brands" -> {
                ThuongHieu item = brands.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(brands.save(item));
            }
            case "materials" -> {
                ChatLieu item = materials.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(materials.save(item));
            }
            case "origins" -> {
                XuatXu item = origins.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(origins.save(item));
            }
            case "collars" -> {
                CoGiay item = collars.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(collars.save(item));
            }
            case "styles" -> {
                KieuDang item = styles.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status);
                return response(styles.save(item));
            }
            case "colors" -> {
                MauSac item = colors.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status); item.setNgayCapNhat(LocalDateTime.now());
                return response(colors.save(item));
            }
            case "sizes" -> {
                KichThuoc item = sizes.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
                item.setTrangThai(status); item.setNgayCapNhat(LocalDateTime.now());
                return response(sizes.save(item));
            }
            default -> throw ProductException.badRequest("Loại thuộc tính không hợp lệ");
        }
    }

    private AttributeResponse saveDanhMuc(Long id, AttributeRequest r) {
        if (categories.exists(duplicate(id, "maDanhMuc", "tenDanhMuc", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        DanhMuc item = id == null ? new DanhMuc() : categories.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaDanhMuc(ProductRules.trim(r.ma()));
        item.setTenDanhMuc(ProductRules.trim(r.ten()));
        item.setMoTa(ProductRules.trim(r.ghiChu()));
        item.setTrangThai(r.trangThai());
        return response(categories.save(item));
    }

    private AttributeResponse response(DanhMuc item) {
        return new AttributeResponse(item.getId(), item.getMaDanhMuc(), item.getTenDanhMuc(),
                item.getMoTa(), null, item.getTrangThai());
    }

    private AttributeResponse saveThuongHieu(Long id, AttributeRequest r) {
        if (brands.exists(duplicate(id, "maThuongHieu", "tenThuongHieu", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        ThuongHieu item = id == null ? new ThuongHieu() : brands.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaThuongHieu(ProductRules.trim(r.ma()));
        item.setTenThuongHieu(ProductRules.trim(r.ten()));
        item.setTrangThai(r.trangThai());
        return response(brands.save(item));
    }

    private AttributeResponse response(ThuongHieu item) {
        return new AttributeResponse(item.getId(), item.getMaThuongHieu(), item.getTenThuongHieu(),
                null, null, item.getTrangThai());
    }

    private AttributeResponse saveChatLieu(Long id, AttributeRequest r) {
        if (materials.exists(duplicate(id, "maChatLieu", "tenChatLieu", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        ChatLieu item = id == null ? new ChatLieu() : materials.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaChatLieu(ProductRules.trim(r.ma()));
        item.setTenChatLieu(ProductRules.trim(r.ten()));
        item.setTrangThai(r.trangThai());
        return response(materials.save(item));
    }

    private AttributeResponse response(ChatLieu item) {
        return new AttributeResponse(item.getId(), item.getMaChatLieu(), item.getTenChatLieu(),
                null, null, item.getTrangThai());
    }

    private AttributeResponse saveXuatXu(Long id, AttributeRequest r) {
        if (origins.exists(duplicate(id, "maXuatXu", "tenXuatXu", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        XuatXu item = id == null ? new XuatXu() : origins.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaXuatXu(ProductRules.trim(r.ma()));
        item.setTenXuatXu(ProductRules.trim(r.ten()));
        item.setTrangThai(r.trangThai());
        return response(origins.save(item));
    }

    private AttributeResponse response(XuatXu item) {
        return new AttributeResponse(item.getId(), item.getMaXuatXu(), item.getTenXuatXu(),
                null, null, item.getTrangThai());
    }

    private AttributeResponse saveCoGiay(Long id, AttributeRequest r) {
        if (collars.exists(duplicate(id, "maCoGiay", "tenCoGiay", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        CoGiay item = id == null ? new CoGiay() : collars.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaCoGiay(ProductRules.trim(r.ma()));
        item.setTenCoGiay(ProductRules.trim(r.ten()));
        item.setTrangThai(r.trangThai());
        return response(collars.save(item));
    }

    private AttributeResponse response(CoGiay item) {
        return new AttributeResponse(item.getId(), item.getMaCoGiay(), item.getTenCoGiay(),
                null, null, item.getTrangThai());
    }

    private AttributeResponse saveKieuDang(Long id, AttributeRequest r) {
        if (styles.exists(duplicate(id, "maKieuDang", "tenKieuDang", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        KieuDang item = id == null ? new KieuDang() : styles.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaKieuDang(ProductRules.trim(r.ma()));
        item.setTenKieuDang(ProductRules.trim(r.ten()));
        item.setTrangThai(r.trangThai());
        return response(styles.save(item));
    }

    private AttributeResponse response(KieuDang item) {
        return new AttributeResponse(item.getId(), item.getMaKieuDang(), item.getTenKieuDang(),
                null, null, item.getTrangThai());
    }

    private AttributeResponse saveMauSac(Long id, AttributeRequest r) {
        if (colors.exists(duplicate(id, "maMauSac", "tenMauSac", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        MauSac item = id == null ? new MauSac() : colors.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setMaMauSac(ProductRules.trim(r.ma()));
        item.setTenMauSac(ProductRules.trim(r.ten()));
        item.setMaMauHex(ProductRules.trim(r.maMauHex()));
        item.setTrangThai(r.trangThai());
        if (id == null) item.setNgayTao(LocalDateTime.now()); else item.setNgayCapNhat(LocalDateTime.now());
        return response(colors.save(item));
    }

    private AttributeResponse response(MauSac item) {
        return new AttributeResponse(item.getId(), item.getMaMauSac(), item.getTenMauSac(),
                null, item.getMaMauHex(), item.getTrangThai());
    }

    private AttributeResponse saveKichThuoc(Long id, AttributeRequest r) {
        if (sizes.exists(duplicate(id, null, "giaTri", r)))
            throw ProductException.conflict("Mã hoặc tên/giá trị thuộc tính đã tồn tại");
        KichThuoc item = id == null ? new KichThuoc() : sizes.findById(id)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy thuộc tính"));
        item.setGiaTri(ProductRules.trim(r.ten()));
        item.setGhiChu(ProductRules.trim(r.ghiChu()));
        item.setTrangThai(r.trangThai());
        if (id == null) item.setNgayTao(LocalDateTime.now()); else item.setNgayCapNhat(LocalDateTime.now());
        return response(sizes.save(item));
    }

    private AttributeResponse response(KichThuoc item) {
        return new AttributeResponse(item.getId(), null, item.getGiaTri(),
                item.getGhiChu(), null, item.getTrangThai());
    }
}

