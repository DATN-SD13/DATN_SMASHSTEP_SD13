package com.smashstep.datn.common.pricing;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class CurrentPriceServiceTest {
    private final EntityManager em = mock(EntityManager.class);
    private final CurrentPriceService service = new CurrentPriceService(em);
    private final SanPhamChiTiet variant = variant();
    private SanPhamChiTiet variant() {
        var v = new SanPhamChiTiet(); v.setId(1L); v.setGiaBan(new BigDecimal("2200000")); return v;
    }
    private ChiTietDotGiamGia campaign(String percent, String override) {
        var d = new DotGiamGia(); d.setId(1L); d.setMaDotGiamGia("DGG001"); d.setTenDotGiamGia("Campaign"); d.setPhanTramGiamDot(new BigDecimal(percent));
        var c = new ChiTietDotGiamGia(); c.setIdDotGiamGia(d); c.setIdSanPhamChiTiet(variant);
        c.setPhanTramGiamBienThe(override == null ? null : new BigDecimal(override)); return c;
    }
    private void campaigns(ChiTietDotGiamGia... rows) {
        TypedQuery<ChiTietDotGiamGia> q = mock(TypedQuery.class, RETURNS_SELF);
        when(em.createQuery(anyString(), eq(ChiTietDotGiamGia.class))).thenReturn(q);
        when(q.getResultList()).thenReturn(List.of(rows));
    }
    @Test void usesHighestValidOverrideWithoutCompoundingAndOneBulkQuery() {
        campaigns(campaign("10", null), campaign("10", "12"), campaign("20", null));
        var prices = service.getPrices(List.of(variant), LocalDateTime.now()); var p = prices.get(1L);
        assertEquals(new BigDecimal("1760000.00"), p.getEffectivePrice());
        assertEquals(new BigDecimal("20"), p.getDiscountPercent()); assertTrue(p.isDiscounted());
        assertEquals(new BigDecimal("2200000"), variant.getGiaBan());
        verify(em, times(1)).createQuery(anyString(), eq(ChiTietDotGiamGia.class));
    }
    @Test void ignoresInvalidLegacyDiscountsAndNeverProducesNegativePrices() {
        campaigns(campaign("101", null), campaign("-10", null), campaign("10", "0"));
        var p = service.getPrice(variant, LocalDateTime.now());
        assertEquals(new BigDecimal("2200000.00"), p.getEffectivePrice()); assertFalse(p.isDiscounted());
        campaigns(campaign("100", null)); assertEquals(new BigDecimal("0.00"), service.getPrice(variant, LocalDateTime.now()).getEffectivePrice());
    }
    @Test void roundsHalfUpAtTwoDecimalsAndUsesDetailOverride() {
        variant.setGiaBan(new BigDecimal("1.05")); campaigns(campaign("10", "50"));
        assertEquals(new BigDecimal("0.53"), service.getPrice(variant, LocalDateTime.now()).getEffectivePrice());
    }
}
