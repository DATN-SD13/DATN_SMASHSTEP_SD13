package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.dto.*;
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
public class SanPhamChiTietService {
    private final SanPhamChiTietRepository variants;
    private final SanPhamRepository products;
    private final MauSacRepository colors;
    private final KichThuocRepository sizes;
    private final HinhAnhSanPhamRepository images;

    public PageResponse<VariantResponse> getVariants(int page, int size, String keyword, Integer status,
            Long productId, Long colorId, Long sizeId) {
        return getVariants(page, size, keyword, status, productId, colorId, sizeId, null);
    }

    public PageResponse<VariantResponse> getVariants(int page, int size, String keyword, Integer status,
            Long productId, Long colorId, Long sizeId, Boolean active) {
        ProductRules.status(status);
        Specification<SanPhamChiTiet> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!ProductRules.trim(keyword).isEmpty()) {
                String pattern = ProductRules.like(keyword);
                predicates.add(cb.or(cb.like(cb.lower(root.get("sku")), pattern, '\\'),
                        cb.like(cb.lower(root.get("maChiTietSanPham")), pattern, '\\'),
                        cb.like(cb.lower(root.get("idSanPham").get("maSanPham")), pattern, '\\'),
                        cb.like(cb.lower(root.get("idSanPham").get("tenSanPham")), pattern, '\\')));
            }
            if (status != null) predicates.add(cb.equal(root.get("trangThai"), status));
            if (active != null) predicates.add(cb.equal(root.get("kichHoat"), active));
            if (productId != null) predicates.add(cb.equal(root.get("idSanPham").get("id"), productId));
            if (colorId != null) predicates.add(cb.equal(root.get("idMauSac").get("id"), colorId));
            if (sizeId != null) predicates.add(cb.equal(root.get("idKichThuoc").get("id"), sizeId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = variants.findAll(filter, ProductRules.page(page, size));
        Map<Long, String> mainImages = HinhAnhSanPhamService.mainImageUrls(images,
                result.getContent().stream().map(v -> v.getIdSanPham().getId()).distinct().toList());
        return PageResponse.from(result.map(v -> withImage(v, mainImages.get(v.getIdSanPham().getId()))));
    }

    public List<VariantResponse> getVariantsByProduct(Long productId) {
        products.findById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        String mainImage = HinhAnhSanPhamService.mainImageUrls(images, List.of(productId)).get(productId);
        return variants.findByIdSanPham_IdOrderByIdAsc(productId).stream()
                .map(v -> withImage(v, mainImage)).toList();
    }

    public VariantResponse getVariantById(Long id) {
        SanPhamChiTiet variant = find(id);
        Long productId = variant.getIdSanPham().getId();
        return withImage(variant, HinhAnhSanPhamService.mainImageUrls(images, List.of(productId)).get(productId));
    }

    static VariantResponse withImage(SanPhamChiTiet variant, String mainImage) {
        VariantResponse response = toResponse(variant);
        response.setAnhChinh(mainImage);
        return response;
    }

    @Transactional
    public VariantResponse createVariant(VariantCreateRequest request) {
        if (request.getSanPhamId() == null) throw ProductException.badRequest("ID sản phẩm không được trống");
        return createVariant(request.getSanPhamId(), request);
    }

    @Transactional
    public VariantResponse createVariant(Long productId, VariantCreateRequest request) {
        if (request.getSanPhamId() != null && !productId.equals(request.getSanPhamId()))
            throw ProductException.badRequest("Biến thể phải thuộc sản phẩm đang chọn");
        request.setSanPhamId(productId);
        return createOne(lockProduct(productId), request);
    }

    @Transactional
    public List<VariantResponse> createBatch(Long productId, List<VariantCreateRequest> requests) {
        SanPham product = lockProduct(productId);
        Set<String> codes = new HashSet<>(), skus = new HashSet<>(), combinations = new HashSet<>();
        for (VariantCreateRequest r : requests) {
            if (r.getSanPhamId() != null && !productId.equals(r.getSanPhamId()))
                throw ProductException.badRequest("Biến thể phải thuộc sản phẩm đang chọn");
            r.setSanPhamId(productId);
            if (!codes.add(ProductRules.trim(r.getMaChiTietSanPham()).toLowerCase(Locale.ROOT)))
                throw ProductException.conflict("Mã biến thể trùng trong danh sách tạo");
            if (!skus.add(ProductRules.trim(r.getSku()).toLowerCase(Locale.ROOT)))
                throw ProductException.conflict("SKU trùng trong danh sách tạo");
            if (!combinations.add(r.getMauSacId() + ":" + r.getKichThuocId()))
                throw ProductException.conflict("Tổ hợp màu sắc và kích thước trùng trong danh sách tạo");
        }
        List<VariantResponse> result = new ArrayList<>();
        for (VariantCreateRequest request : requests) result.add(createOne(product, request));
        return result;
    }

    private VariantResponse createOne(SanPham product, VariantCreateRequest r) {
        SanPhamChiTiet v = new SanPhamChiTiet();
        v.setIdSanPham(product);
        v.setNgayTao(LocalDateTime.now());
        apply(v, r);
        return toResponse(variants.save(v));
    }

    @Transactional
    public VariantResponse updateVariant(Long id, VariantUpdateRequest update) {
        VariantCreateRequest request = update.toVariantCreateRequest();
        SanPhamChiTiet v = find(id);
        if (request.getSanPhamId() == null) request.setSanPhamId(v.getIdSanPham().getId());
        if (request.getMaChiTietSanPham() == null) request.setMaChiTietSanPham(v.getMaChiTietSanPham());
        if (!v.getIdSanPham().getId().equals(request.getSanPhamId()))
            throw ProductException.badRequest("Không thể chuyển biến thể sang sản phẩm khác");
        lockProduct(request.getSanPhamId());
        apply(v, request);
        v.setNgayCapNhat(LocalDateTime.now());
        return toResponse(variants.save(v));
    }

    @Transactional
    public VariantResponse updateStatus(Long id, Integer status) {
        ProductRules.status(status);
        SanPhamChiTiet v = find(id);
        lockProduct(v.getIdSanPham().getId());
        v.setTrangThai(status);
        v.setNgayCapNhat(LocalDateTime.now());
        return toResponse(variants.save(v));
    }

    private void apply(SanPhamChiTiet v, VariantCreateRequest r) {
        Long id = v.getId() == null ? 0L : v.getId();
        String code = ProductRules.trim(r.getMaChiTietSanPham()), sku = ProductRules.trim(r.getSku());
        if (variants.existsByMaChiTietSanPhamIgnoreCaseAndIdNot(code, id))
            throw ProductException.conflict("Mã biến thể đã tồn tại");
        if (variants.existsBySkuIgnoreCaseAndIdNot(sku, id))
            throw ProductException.conflict("SKU đã tồn tại");
        if (variants.existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_IdAndIdNot(r.getSanPhamId(), r.getMauSacId(), r.getKichThuocId(), id))
            throw ProductException.conflict("Sản phẩm đã có biến thể với màu sắc và kích thước này");
        MauSac color = colors.findById(r.getMauSacId()).orElseThrow(() -> ProductException.badRequest("Màu sắc không tồn tại"));
        KichThuoc size = sizes.findById(r.getKichThuocId()).orElseThrow(() -> ProductException.badRequest("Kích thước không tồn tại"));
        SanPhamService.selected(color.getTrangThai(), v.getIdMauSac() == null ? null : v.getIdMauSac().getId(), color.getId());
        SanPhamService.selected(size.getTrangThai(), v.getIdKichThuoc() == null ? null : v.getIdKichThuoc().getId(), size.getId());
        v.setMaChiTietSanPham(code); v.setSku(sku); v.setIdMauSac(color); v.setIdKichThuoc(size);
        v.setGiaBan(r.getGiaBan()); v.setSoLuong(r.getSoLuong()); v.setKichHoat(r.getKichHoat()); v.setTrangThai(r.getTrangThai());
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
                v.getSoLuong(), v.getGiaBan(), v.getKichHoat(), v.getTrangThai(), v.getNgayTao(), v.getNgayCapNhat(), null,
                v.getIdMauSac() == null ? null : v.getIdMauSac().getMaMauSac());
    }
}
