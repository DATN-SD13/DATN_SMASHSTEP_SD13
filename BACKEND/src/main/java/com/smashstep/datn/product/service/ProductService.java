package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final SanPhamRepository products;
    private final SanPhamChiTietRepository variants;
    private final HinhAnhSanPhamRepository images;
    private final DanhMucRepository categories;
    private final ThuongHieuRepository brands;
    private final ChatLieuRepository materials;
    private final KieuDangRepository styles;
    private final CoGiayRepository collars;
    private final XuatXuRepository origins;

    public PageResponse<ProductResponse> list(int page, int size, String keyword, Integer status,
            Long categoryId, Long brandId, Long materialId, Long styleId, Long collarId, Long originId) {
        ProductRules.status(status);
        Specification<SanPham> filter = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (!ProductRules.trim(keyword).isEmpty()) {
                String pattern = ProductRules.like(keyword);
                conditions.add(cb.or(cb.like(cb.lower(root.get("maSanPham")), pattern, '\\'),
                        cb.like(cb.lower(root.get("tenSanPham")), pattern, '\\')));
            }
            if (status != null) conditions.add(cb.equal(root.get("trangThai"), status));
            Map<String, Long> attributes = new LinkedHashMap<>();
            attributes.put("idDanhMuc", categoryId); attributes.put("idThuongHieu", brandId);
            attributes.put("idChatLieu", materialId); attributes.put("idKieuDang", styleId);
            attributes.put("idCoGiay", collarId); attributes.put("idXuatXu", originId);
            attributes.forEach((field, id) -> {
                if (id != null) conditions.add(cb.equal(root.get(field).get("id"), id));
            });
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        Page<SanPham> result = products.findAll(filter, ProductRules.page(page, size));
        Map<Long, InventorySummary> totals = summaries(result.getContent().stream().map(SanPham::getId).toList());
        return PageResponse.from(result.map(p -> toResponse(p, totals.get(p.getId()))));
    }

    public ProductDetail detail(Long id) {
        SanPham product = find(id);
        InventorySummary total = summaries(List.of(id)).get(id);
        return new ProductDetail(toResponse(product, total),
                variants.findByIdSanPham_IdOrderByIdAsc(id).stream().map(VariantService::toResponse).toList(),
                images.findByIdSanPham_IdOrderByIdAsc(id).stream().map(this::imageResponse).toList());
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String code = ProductRules.trim(request.maSanPham());
        if (products.existsByMaSanPhamIgnoreCase(code)) throw ProductException.conflict("Mã sản phẩm đã tồn tại");
        SanPham product = new SanPham();
        product.setMaSanPham(code);
        product.setNgayTao(LocalDateTime.now());
        apply(product, request);
        return toResponse(products.save(product), null);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        SanPham product = products.findLockedById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        if (!Objects.equals(product.getMaSanPham(), ProductRules.trim(request.maSanPham())))
            throw ProductException.badRequest("Mã sản phẩm không được thay đổi khi sửa");
        apply(product, request);
        product.setNgayCapNhat(LocalDateTime.now());
        return toResponse(products.save(product), summaries(List.of(id)).get(id));
    }

    @Transactional
    public ProductResponse changeStatus(Long id, Integer status) {
        ProductRules.status(status);
        SanPham product = products.findLockedById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        product.setTrangThai(status);
        product.setNgayCapNhat(LocalDateTime.now());
        return toResponse(products.save(product), summaries(List.of(id)).get(id));
    }

    private void apply(SanPham p, ProductRequest r) {
        DanhMuc category = categories.findById(r.danhMucId()).orElseThrow(() -> ProductException.badRequest("Danh mục không tồn tại"));
        ThuongHieu brand = brands.findById(r.thuongHieuId()).orElseThrow(() -> ProductException.badRequest("Thương hiệu không tồn tại"));
        ChatLieu material = materials.findById(r.chatLieuId()).orElseThrow(() -> ProductException.badRequest("Chất liệu không tồn tại"));
        KieuDang style = styles.findById(r.kieuDangId()).orElseThrow(() -> ProductException.badRequest("Kiểu dáng không tồn tại"));
        CoGiay collar = collars.findById(r.coGiayId()).orElseThrow(() -> ProductException.badRequest("Cổ giày không tồn tại"));
        XuatXu origin = origins.findById(r.xuatXuId()).orElseThrow(() -> ProductException.badRequest("Xuất xứ không tồn tại"));
        selected(category.getTrangThai(), p.getIdDanhMuc() == null ? null : p.getIdDanhMuc().getId(), category.getId());
        selected(brand.getTrangThai(), p.getIdThuongHieu() == null ? null : p.getIdThuongHieu().getId(), brand.getId());
        selected(material.getTrangThai(), p.getIdChatLieu() == null ? null : p.getIdChatLieu().getId(), material.getId());
        selected(style.getTrangThai(), p.getIdKieuDang() == null ? null : p.getIdKieuDang().getId(), style.getId());
        selected(collar.getTrangThai(), p.getIdCoGiay() == null ? null : p.getIdCoGiay().getId(), collar.getId());
        selected(origin.getTrangThai(), p.getIdXuatXu() == null ? null : p.getIdXuatXu().getId(), origin.getId());
        p.setIdDanhMuc(category); p.setIdThuongHieu(brand); p.setIdChatLieu(material);
        p.setIdKieuDang(style); p.setIdCoGiay(collar); p.setIdXuatXu(origin);
        p.setTenSanPham(ProductRules.trim(r.tenSanPham()));
        p.setMoTaChiTiet(ProductRules.trim(r.moTaChiTiet()));
        p.setTrangThai(r.trangThai());
    }

    static void selected(Integer status, Long currentId, Long selectedId) {
        if (!Objects.equals(status, 1) && !Objects.equals(currentId, selectedId))
            throw ProductException.badRequest("Thuộc tính được chọn đã ngừng hoạt động");
    }

    private SanPham find(Long id) {
        return products.findById(id).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
    }

    private Map<Long, InventorySummary> summaries(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return variants.summarize(ids).stream().collect(Collectors.toMap(InventorySummary::getProductId, Function.identity()));
    }

    private ProductResponse toResponse(SanPham p, InventorySummary total) {
        return new ProductResponse(p.getId(), p.getMaSanPham(), p.getTenSanPham(),
                p.getIdDanhMuc() == null ? null : p.getIdDanhMuc().getId(), p.getIdDanhMuc() == null ? null : p.getIdDanhMuc().getTenDanhMuc(),
                p.getIdThuongHieu() == null ? null : p.getIdThuongHieu().getId(), p.getIdThuongHieu() == null ? null : p.getIdThuongHieu().getTenThuongHieu(),
                p.getIdChatLieu() == null ? null : p.getIdChatLieu().getId(), p.getIdChatLieu() == null ? null : p.getIdChatLieu().getTenChatLieu(),
                p.getIdKieuDang() == null ? null : p.getIdKieuDang().getId(), p.getIdKieuDang() == null ? null : p.getIdKieuDang().getTenKieuDang(),
                p.getIdCoGiay() == null ? null : p.getIdCoGiay().getId(), p.getIdCoGiay() == null ? null : p.getIdCoGiay().getTenCoGiay(),
                p.getIdXuatXu() == null ? null : p.getIdXuatXu().getId(), p.getIdXuatXu() == null ? null : p.getIdXuatXu().getTenXuatXu(),
                p.getMoTaChiTiet(), p.getTrangThai(), p.getNgayTao(), p.getNgayCapNhat(),
                total == null || total.getQuantity() == null ? 0 : total.getQuantity(),
                total == null ? null : total.getMinPrice(), total == null ? null : total.getMaxPrice(),
                total == null ? 0 : total.getVariantCount());
    }

    @Transactional
    public ImageResponse addImage(Long productId, ImageRequest request) {
        SanPham p = products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        String url = imageUrl(request.urlAnh());
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        boolean main = request.isAnhChinh() || current.isEmpty();
        if (main) current.forEach(image -> image.setIsAnhChinh(false));
        HinhAnhSanPham image = new HinhAnhSanPham();
        image.setIdSanPham(p); image.setUrlAnh(url); image.setIsAnhChinh(main);
        return imageResponse(images.save(image));
    }

    @Transactional
    public ImageResponse updateImage(Long productId, Long imageId, ImageRequest request) {
        products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        HinhAnhSanPham image = current.stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh của sản phẩm"));
        image.setUrlAnh(imageUrl(request.urlAnh()));
        // Keep one main image when editing; select another image to replace the main one.
        boolean main = request.isAnhChinh() || Boolean.TRUE.equals(image.getIsAnhChinh());
        if (main) current.forEach(i -> i.setIsAnhChinh(i.getId().equals(imageId)));
        return imageResponse(images.save(image));
    }

    @Transactional
    public void removeImage(Long productId, Long imageId) {
        products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        HinhAnhSanPham target = current.stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh của sản phẩm"));
        images.delete(target);
        if (Boolean.TRUE.equals(target.getIsAnhChinh()))
            current.stream().filter(i -> !i.getId().equals(imageId)).findFirst().ifPresent(i -> i.setIsAnhChinh(true));
    }

    private String imageUrl(String input) {
        String url = ProductRules.trim(input);
        try {
            URI uri = URI.create(url);
            if (!Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw ProductException.badRequest("URL ảnh phải là địa chỉ HTTP/HTTPS hợp lệ"); }
        return url;
    }

    private ImageResponse imageResponse(HinhAnhSanPham image) {
        return new ImageResponse(image.getId(), image.getUrlAnh(), image.getIsAnhChinh());
    }
}

