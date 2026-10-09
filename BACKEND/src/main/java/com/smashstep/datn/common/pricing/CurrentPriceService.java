package com.smashstep.datn.common.pricing;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/** Read-only campaign pricing. Product base prices and historical invoice prices never change here. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentPriceService {
    private final EntityManager em;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public List<SanPhamChiTiet> getProductVariants(Collection<Long> productIds) {
        if (productIds.isEmpty()) return List.of();
        return em.createQuery("select v from SanPhamChiTiet v join fetch v.idSanPham"
                + " left join fetch v.idMauSac left join fetch v.idKichThuoc"
                + " where v.idSanPham.id in :ids order by v.id", SanPhamChiTiet.class)
                .setParameter("ids", productIds).getResultList();
    }

    public CurrentPrice getPrice(SanPhamChiTiet variant, LocalDateTime now) {
        return getPrices(List.of(variant), now).get(variant.getId());
    }

    public Map<Long, CurrentPrice> getPrices(Collection<SanPhamChiTiet> variants, LocalDateTime now) {
        Map<Long, CurrentPrice> result = new LinkedHashMap<>();
        if (variants.isEmpty()) return result;
        List<Long> ids = variants.stream().map(SanPhamChiTiet::getId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ChiTietDotGiamGia> best = new HashMap<>();
        if (!ids.isEmpty()) {
            List<ChiTietDotGiamGia> campaigns = em.createQuery("select c from ChiTietDotGiamGia c join fetch c.idDotGiamGia d"
                    + " where c.idSanPhamChiTiet.id in :ids and c.trangThai = 1 and d.trangThai = 1"
                    + " and d.kichHoat = true and d.ngayBatDau <= :now and d.ngayKetThuc >= :now order by d.id,c.id", ChiTietDotGiamGia.class)
                    .setParameter("ids", ids).setParameter("now", now).getResultList();
            for (ChiTietDotGiamGia detail : campaigns) {
                BigDecimal percent = percent(detail);
                // Invalid legacy percentages are ignored, never compounded or allowed to produce negative prices.
                if (percent == null || percent.signum() <= 0 || percent.compareTo(HUNDRED) > 0) continue;
                Long id = detail.getIdSanPhamChiTiet().getId();
                ChiTietDotGiamGia previous = best.get(id);
                if (previous == null || percent.compareTo(percent(previous)) > 0) best.put(id, detail);
            }
        }
        for (SanPhamChiTiet variant : variants) {
            BigDecimal original = variant.getGiaBan() == null ? BigDecimal.ZERO : variant.getGiaBan();
            ChiTietDotGiamGia detail = best.get(variant.getId());
            BigDecimal discount = detail == null ? BigDecimal.ZERO : percent(detail);
            BigDecimal effective = original.max(BigDecimal.ZERO).multiply(BigDecimal.ONE.subtract(discount.divide(HUNDRED)))
                    .setScale(2, RoundingMode.HALF_UP);
            result.put(variant.getId(), new CurrentPrice(original, effective, discount, detail != null,
                    detail == null ? null : detail.getIdDotGiamGia().getId(),
                    detail == null ? null : detail.getIdDotGiamGia().getMaDotGiamGia(),
                    detail == null ? null : detail.getIdDotGiamGia().getTenDotGiamGia()));
        }
        return result;
    }

    private BigDecimal percent(ChiTietDotGiamGia detail) {
        return detail.getPhanTramGiamBienThe() != null ? detail.getPhanTramGiamBienThe() : detail.getIdDotGiamGia().getPhanTramGiamDot();
    }
}
