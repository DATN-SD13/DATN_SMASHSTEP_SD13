package com.smashstep.datn.storefront.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPrice;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.service.QuyTacSanPham;
import com.smashstep.datn.storefront.dto.*;
import com.smashstep.datn.storefront.dto.StorefrontFilterResponse.Option;
import com.smashstep.datn.storefront.dto.StorefrontProductDetailResponse.Image;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Reads existing entities. No writes, schema changes, persisted sale fields or independent campaign rules. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontProductService {
    private final EntityManager em;
    private final CurrentPriceService currentPrices;
    private final com.smashstep.datn.storefront.review.ProductReviewService reviews;
    private static final int BATCH = 250;
    private static final String ELIGIBLE = "v.trangThai = 1 and v.kichHoat = true and v.giaBan > 0"
            + " and p.trangThai = 1 and v.idMauSac is not null and v.idKichThuoc is not null";
    private static final String FETCH = "select v from SanPhamChiTiet v join fetch v.idSanPham p"
            + " left join fetch p.idThuongHieu left join fetch p.idDanhMuc left join fetch p.idChatLieu"
            + " left join fetch p.idKieuDang left join fetch p.idCoGiay left join fetch p.idXuatXu"
            + " left join fetch v.idMauSac left join fetch v.idKichThuoc";

    public PageResponse<StorefrontProductResponse> list(StorefrontQuery input) {
        validate(input);
        StringBuilder where = new StringBuilder(ELIGIBLE);
        Map<String,Object> params = new LinkedHashMap<>();
        if (input.getKeyword() != null && !input.getKeyword().isBlank()) {
            where.append(" and (lower(p.tenSanPham) like :keyword escape '\\' or lower(p.maSanPham) like :keyword escape '\\')");
            params.put("keyword", QuyTacSanPham.taoMauTimKiem(input.getKeyword()));
        }
        filter(where, params, "brand", "p.idThuongHieu.id", input.getBrandId());
        filter(where, params, "category", "p.idDanhMuc.id", input.getCategoryId());
        filter(where, params, "color", "v.idMauSac.id", input.getColorId());
        filter(where, params, "size", "v.idKichThuoc.id", input.getSizeId());
        filter(where, params, "style", "p.idKieuDang.id", input.getStyleId());
        var grouped = new LinkedHashMap<Long, Summary>();
        LocalDateTime now = LocalDateTime.now();
        long after = 0;
        // Current campaign prices cannot be replaced by a separate SQL pricing formula.
        // Scan variants in bounded batches; aggregate lightweight product summaries, then paginate cards.
        while (true) {
            var query = em.createQuery(FETCH + " where " + where + " and v.id > :after order by v.id", SanPhamChiTiet.class);
            params.forEach(query::setParameter);
            List<SanPhamChiTiet> batch = query.setParameter("after", after).setMaxResults(BATCH).getResultList();
            if (batch.isEmpty()) break;
            var prices = currentPrices.getPrices(batch, now);
            for (var variant : batch) {
                CurrentPrice price = prices.get(variant.getId());
                if (!withinPrice(price.getEffectivePrice(), input)) continue;
                grouped.computeIfAbsent(variant.getIdSanPham().getId(), id -> new Summary(variant.getIdSanPham()))
                        .add(variant, price);
            }
            after = batch.get(batch.size() - 1).getId();
            if (batch.size() < BATCH) break;
        }
        var summaries = new ArrayList<>(grouped.values());
        Comparator<Summary> newest = Comparator.comparing((Summary s) -> s.product.getNgayTao(),
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(s -> s.product.getId(), Comparator.reverseOrder());
        Comparator<Summary> ordering = switch (input.getSort()) {
            case "priceAsc" -> Comparator.comparing((Summary s) -> s.price.getEffectivePrice()).thenComparing(newest);
            case "priceDesc" -> Comparator.comparing((Summary s) -> s.price.getEffectivePrice(), Comparator.reverseOrder()).thenComparing(newest);
            case "featured" -> {
                Set<Long> illustrated = new HashSet<>(em.createQuery(
                        "select distinct h.idSanPham.id from HinhAnhSanPham h left join h.idSanPhamChiTiet iv where h.urlAnh is not null"
                        + " and (iv.id is null or (iv.trangThai = 1 and iv.kichHoat = true))",
                        Long.class).getResultList());
                yield Comparator.comparing((Summary s) -> illustrated.contains(s.product.getId())).reversed()
                        .thenComparing(Comparator.comparing((Summary s) -> s.price.isDiscounted()).reversed()).thenComparing(newest);
            }
            default -> newest;
        };
        summaries.sort(ordering);
        int total = summaries.size(), from = (int) Math.min((long) input.getPage() * input.getSize(), total);
        var page = summaries.subList(from, Math.min(from + input.getSize(), total));
        var pictures = images(page.stream().map(s -> s.product.getId()).toList());
        var ratings = reviews.summaries(page.stream().map(s -> s.product.getId()).toList());
        var content = page.stream().map(s -> response(s, pictures, ratings)).toList();
        return new PageResponse<>(content, input.getPage(), input.getSize(), total, (total + input.getSize() - 1) / input.getSize());
    }

    public StorefrontProductDetailResponse detail(Long id) {
        if (id == null || id <= 0) throw AppException.notFound("Không tìm thấy sản phẩm");
        var rows = em.createQuery(FETCH + " where " + ELIGIBLE + " and p.id = :id order by v.id", SanPhamChiTiet.class)
                .setParameter("id", id).getResultList();
        if (rows.isEmpty()) throw AppException.notFound("Không tìm thấy sản phẩm đang kinh doanh");
        var prices = currentPrices.getPrices(rows, LocalDateTime.now());
        var summary = new Summary(rows.get(0).getIdSanPham());
        rows.forEach(v -> summary.add(v, prices.get(v.getId())));
        var allImages = images(List.of(id));
        Set<Long> eligibleIds = rows.stream().map(SanPhamChiTiet::getId).collect(Collectors.toSet());
        var visibleImages = allImages.stream().filter(i -> i.variantId() == null || eligibleIds.contains(i.variantId())).toList();
        var variants = rows.stream().map(v -> {
            var price = prices.get(v.getId());
            var own = visibleImages.stream().filter(i -> Objects.equals(i.variantId(), v.getId())).map(Image::urlAnh).toList();
            return new StorefrontVariantResponse(v.getId(), v.getMaChiTietSanPham(), v.getSku(),
                    v.getIdMauSac().getId(), v.getIdMauSac().getTenMauSac(), v.getIdMauSac().getMaMauHex(),
                    v.getIdKichThuoc().getId(), v.getIdKichThuoc().getGiaTri(), Math.max(0, Objects.requireNonNullElse(v.getSoLuong(), 0)),
                    price.getOriginalPrice(), price.getEffectivePrice(), price.getDiscountPercent(), price.isDiscounted(),
                    mainImage(visibleImages, v.getId()), own);
        }).toList();
        return new StorefrontProductDetailResponse(response(summary, visibleImages, reviews.summaries(List.of(id))), variants, visibleImages);
    }

    public StorefrontFilterResponse filters() {
        return new StorefrontFilterResponse(brands(), options("p.idDanhMuc", "tenDanhMuc"),
                options("v.idMauSac", "tenMauSac"), options("v.idKichThuoc", "giaTri"), options("p.idKieuDang", "tenKieuDang"));
    }

    public List<Option> brands() {
        var rows = em.createQuery("select b.id, b.tenThuongHieu, count(distinct p.id),"
                + " min(case when iv.id is null or (iv.trangThai = 1 and iv.kichHoat = true) then h.urlAnh else null end)"
                + " from SanPhamChiTiet v join v.idSanPham p join p.idThuongHieu b"
                + " left join HinhAnhSanPham h on h.idSanPham.id = p.id"
                + " left join h.idSanPhamChiTiet iv"
                + " where " + ELIGIBLE + " group by b.id,b.tenThuongHieu order by b.tenThuongHieu", Object[].class).getResultList();
        return rows.stream().map(row -> new Option((Long) row[0], (String) row[1], (Long) row[2], (String) row[3])).toList();
    }

    private List<Option> options(String association, String name) {
        var rows = em.createQuery("select a.id, a." + name + ", count(distinct p.id)"
                + " from SanPhamChiTiet v join v.idSanPham p join " + association + " a"
                + " where " + ELIGIBLE + " group by a.id,a." + name + " order by a." + name, Object[].class).getResultList();
        return rows.stream().map(row -> new Option((Long) row[0], (String) row[1], (Long) row[2], null)).toList();
    }

    private List<Image> images(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return em.createQuery("select h from HinhAnhSanPham h left join fetch h.idSanPhamChiTiet"
                + " where h.idSanPham.id in :ids order by h.isAnhChinh desc,h.id", HinhAnhSanPham.class)
                .setParameter("ids", ids).getResultList().stream()
                .filter(h -> h.getUrlAnh() != null && !h.getUrlAnh().isBlank())
                .map(h -> new Image(h.getId(), h.getIdSanPham().getId(), h.getUrlAnh(), Boolean.TRUE.equals(h.getIsAnhChinh()),
                        h.getIdSanPhamChiTiet() == null ? null : h.getIdSanPhamChiTiet().getId())).toList();
    }

    private String mainImage(List<Image> images, Long variantId) {
        return images.stream().filter(i -> Objects.equals(i.variantId(), variantId)).findFirst()
                .or(() -> images.stream().filter(i -> i.variantId() == null).findFirst())
                .map(Image::urlAnh).orElse(null);
    }

    private StorefrontProductResponse response(Summary summary, List<Image> allImages,
            Map<Long, com.smashstep.datn.storefront.review.ProductReviewService.Summary> ratings) {
        var p = summary.product;
        var productImages = allImages.stream().filter(i -> Objects.equals(i.productId(), p.getId())
                && (i.variantId() == null || summary.ids.contains(i.variantId()))).toList();
        var price = summary.price;
        var rating = ratings.getOrDefault(p.getId(), new com.smashstep.datn.storefront.review.ProductReviewService.Summary(0,0));
        return new StorefrontProductResponse(p.getId(), p.getMaSanPham(), p.getTenSanPham(),
                p.getIdThuongHieu() == null ? null : p.getIdThuongHieu().getId(), p.getIdThuongHieu() == null ? null : p.getIdThuongHieu().getTenThuongHieu(),
                p.getIdDanhMuc() == null ? null : p.getIdDanhMuc().getId(), p.getIdDanhMuc() == null ? null : p.getIdDanhMuc().getTenDanhMuc(),
                p.getIdChatLieu() == null ? null : p.getIdChatLieu().getTenChatLieu(), p.getIdKieuDang() == null ? null : p.getIdKieuDang().getTenKieuDang(),
                p.getIdCoGiay() == null ? null : p.getIdCoGiay().getTenCoGiay(), p.getIdXuatXu() == null ? null : p.getIdXuatXu().getTenXuatXu(),
                p.getMoTaChiTiet(), mainImage(productImages, summary.cheapestVariantId),
                price.getOriginalPrice(), price.getEffectivePrice(), price.getDiscountPercent(), price.isDiscounted(),
                summary.stock, summary.ids.size(), p.getNgayTao(), summary.maxDiscount, rating.average(), rating.count());
    }

    private void validate(StorefrontQuery q) {
        if (q.getPage() < 0 || q.getSize() < 1 || q.getSize() > 48 || (long) q.getPage() * q.getSize() > Integer.MAX_VALUE)
            throw AppException.badRequest("Phân trang không hợp lệ");
        if (q.getSort() == null || !Set.of("newest", "priceAsc", "priceDesc", "featured").contains(q.getSort()))
            throw AppException.badRequest("Cách sắp xếp không hợp lệ");
        if (q.getKeyword() != null && q.getKeyword().length() > 120) throw AppException.badRequest("Từ khóa tối đa 120 ký tự");
        for (var ids : Arrays.asList(q.getBrandId(), q.getCategoryId(), q.getColorId(), q.getSizeId(), q.getStyleId()))
            if (ids != null && (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id <= 0)))
                throw AppException.badRequest("Bộ lọc không hợp lệ");
        if ((q.getMinPrice() != null && q.getMinPrice().signum() < 0) || (q.getMaxPrice() != null && q.getMaxPrice().signum() < 0)
                || (q.getMinPrice() != null && q.getMaxPrice() != null && q.getMinPrice().compareTo(q.getMaxPrice()) > 0))
            throw AppException.badRequest("Khoảng giá không hợp lệ");
    }

    private boolean withinPrice(BigDecimal price, StorefrontQuery q) {
        return (q.getMinPrice() == null || price.compareTo(q.getMinPrice()) >= 0)
                && (q.getMaxPrice() == null || price.compareTo(q.getMaxPrice()) <= 0);
    }
    private void filter(StringBuilder where, Map<String,Object> params, String param, String field, List<Long> ids) {
        if (ids != null && !ids.isEmpty()) { where.append(" and ").append(field).append(" in :").append(param); params.put(param, ids); }
    }
    private static class Summary {
        final SanPham product;
        final Set<Long> ids = new HashSet<>();
        long stock;
        Long cheapestVariantId;
        CurrentPrice price;
        BigDecimal maxDiscount = BigDecimal.ZERO;
        Summary(SanPham product) { this.product = product; }
        void add(SanPhamChiTiet v, CurrentPrice current) {
            if (current.isDiscounted()) maxDiscount = maxDiscount.max(current.getDiscountPercent());
            if (ids.add(v.getId())) stock += Math.max(0, Objects.requireNonNullElse(v.getSoLuong(), 0));
            if (price == null || current.getEffectivePrice().compareTo(price.getEffectivePrice()) < 0
                    || (current.getEffectivePrice().compareTo(price.getEffectivePrice()) == 0
                    && (current.getDiscountPercent().compareTo(price.getDiscountPercent()) > 0
                    || (current.getDiscountPercent().compareTo(price.getDiscountPercent()) == 0 && v.getId() < cheapestVariantId)))) {
                price = current; cheapestVariantId = v.getId();
            }
        }
    }
}
