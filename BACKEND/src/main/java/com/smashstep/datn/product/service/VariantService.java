package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VariantService {
    private final SanPhamChiTietRepository variants;
    private final SanPhamRepository products;
    private final MauSacRepository colors;
    private final KichThuocRepository sizes;

    public PageResponse<VariantResponse> list(int page, int size, String keyword, Integer status,
            Long productId, Long colorId, Long sizeId) {
        ProductRules.status(status);
        Specification<SanPhamChiTiet> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ProductRules.trim(keyword).isEmpty()) {
                String pattern = ProductRules.like(keyword);
                predicates.add(cb.or(cb.like(cb.lower(root.get("sku")), pattern, '\\'),
                        cb.like(cb.lower(root.get("maChiTietSanPham")), pattern, '\\'),
                        cb.like(cb.lower(root.get("idSanPham").get("tenSanPham")), pattern, '\\')));
            }
            if (status != null) predicates.add(cb.equal(root.get("trangThai"), status));
            if (productId != null) predicates.add(cb.equal(root.get("idSanPham").get("id"), productId));
            if (colorId != null) predicates.add(cb.equal(root.get("idMauSac").get("id"), colorId));
            if (sizeId != null) predicates.add(cb.equal(root.get("idKichThuoc").get("id"), sizeId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(variants.findAll(filter, ProductRules.page(page, size)).map(VariantService::toResponse));
    }

    @Transactional
    public VariantResponse create(VariantRequest request) {
        SanPham product = lockProduct(request.sanPhamId());
        return createOne(product, request);
    }

    @Transactional
    public List<VariantResponse> createBatch(Long productId, List<VariantRequest> requests) {
        SanPham product = lockProduct(productId);
        Set<String> codes = new HashSet<>(), skus = new HashSet<>(), combinations = new HashSet<>();
        for (VariantRequest r : requests) {
            if (!productId.equals(r.sanPhamId())) throw ProductException.badRequest("Biến thể phải thuộc sản phẩm đang chọn");
            if (!codes.add(ProductRules.trim(r.maChiTietSanPham()).toLowerCase(Locale.ROOT)))
                throw ProductException.conflict("Mã biến thể trùng trong danh sách tạo");
            if (!skus.add(ProductRules.trim(r.sku()).toLowerCase(Locale.ROOT)))
                throw ProductException.conflict("SKU trùng trong danh sách tạo");
            if (!combinations.add(r.mauSacId() + ":" + r.kichThuocId()))
                throw ProductException.conflict("Tổ hợp màu sắc và kích thước trùng trong danh sách tạo");
        }
        List<VariantResponse> result = new ArrayList<>();
        for (VariantRequest request : requests) result.add(createOne(product, request));
        return result;
    }

    private VariantResponse createOne(SanPham product, VariantRequest r) {
        SanPhamChiTiet v = new SanPhamChiTiet();
        v.setIdSanPham(product);
        v.setNgayTao(LocalDateTime.now());
        apply(v, r);
        return toResponse(variants.save(v));
    }

    @Transactional
    public VariantResponse update(Long id, VariantRequest request) {
        SanPhamChiTiet v = find(id);
        if (!v.getIdSanPham().getId().equals(request.sanPhamId()))
            throw ProductException.badRequest("Không thể chuyển biến thể sang sản phẩm khác");
        lockProduct(request.sanPhamId());
        apply(v, request);
        v.setNgayCapNhat(LocalDateTime.now());
        return toResponse(variants.save(v));
    }

    @Transactional
    public VariantResponse changeStatus(Long id, Integer status) {
        ProductRules.status(status);
        SanPhamChiTiet v = find(id);
        lockProduct(v.getIdSanPham().getId());
        v.setTrangThai(status);
        v.setNgayCapNhat(LocalDateTime.now());
        return toResponse(variants.save(v));
    }

    private void apply(SanPhamChiTiet v, VariantRequest r) {
        Long id = v.getId() == null ? 0L : v.getId();
        String code = ProductRules.trim(r.maChiTietSanPham()), sku = ProductRules.trim(r.sku());
        if (variants.existsByMaChiTietSanPhamIgnoreCaseAndIdNot(code, id))
            throw ProductException.conflict("Mã biến thể đã tồn tại");
        if (variants.existsBySkuIgnoreCaseAndIdNot(sku, id))
            throw ProductException.conflict("SKU đã tồn tại");
        if (variants.existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_IdAndIdNot(r.sanPhamId(), r.mauSacId(), r.kichThuocId(), id))
            throw ProductException.conflict("Sản phẩm đã có biến thể với màu sắc và kích thước này");
        MauSac color = colors.findById(r.mauSacId()).orElseThrow(() -> ProductException.badRequest("Màu sắc không tồn tại"));
        KichThuoc size = sizes.findById(r.kichThuocId()).orElseThrow(() -> ProductException.badRequest("Kích thước không tồn tại"));
        ProductService.selected(color.getTrangThai(), v.getIdMauSac() == null ? null : v.getIdMauSac().getId(), color.getId());
        ProductService.selected(size.getTrangThai(), v.getIdKichThuoc() == null ? null : v.getIdKichThuoc().getId(), size.getId());
        v.setMaChiTietSanPham(code); v.setSku(sku); v.setIdMauSac(color); v.setIdKichThuoc(size);
        v.setGiaBan(r.giaBan()); v.setSoLuong(r.soLuong()); v.setKichHoat(r.kichHoat()); v.setTrangThai(r.trangThai());
    }

    private SanPham lockProduct(Long id) {
        return products.findLockedById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
    }
    private SanPhamChiTiet find(Long id) {
        return variants.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy biến thể"));
    }

    public static VariantResponse toResponse(SanPhamChiTiet v) {
        return new VariantResponse(v.getId(), v.getIdSanPham().getId(), v.getIdSanPham().getMaSanPham(),
                v.getIdSanPham().getTenSanPham(), v.getMaChiTietSanPham(), v.getSku(),
                v.getIdMauSac() == null ? null : v.getIdMauSac().getId(),
                v.getIdMauSac() == null ? null : v.getIdMauSac().getTenMauSac(),
                v.getIdMauSac() == null ? null : v.getIdMauSac().getMaMauHex(),
                v.getIdKichThuoc() == null ? null : v.getIdKichThuoc().getId(),
                v.getIdKichThuoc() == null ? null : v.getIdKichThuoc().getGiaTri(),
                v.getSoLuong(), v.getGiaBan(), v.getKichHoat(), v.getTrangThai(), v.getNgayTao(), v.getNgayCapNhat());
    }
}

