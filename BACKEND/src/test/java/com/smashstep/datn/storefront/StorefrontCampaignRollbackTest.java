package com.smashstep.datn.storefront;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import com.smashstep.datn.promotion.entity.DotGiamGia;
import com.smashstep.datn.storefront.dto.StorefrontQuery;
import com.smashstep.datn.storefront.dto.StorefrontVariantResponse;
import com.smashstep.datn.storefront.service.StorefrontProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Necessary campaign scenarios absent from live data use transaction fixtures, always rolled back. */
@SpringBootTest(classes=DatnApplication.class, properties={
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false"
})
@Transactional
@Rollback
class StorefrontCampaignRollbackTest {
    @Autowired EntityManager em;
    @Autowired StorefrontProductService store;
    @Autowired CurrentPriceService prices;

    private SanPhamChiTiet variant() {
        var eligible=em.createQuery("select v from SanPhamChiTiet v join fetch v.idSanPham p"
                +" where p.trangThai=1 and v.trangThai=1 and v.kichHoat=true and v.giaBan>0"
                +" and v.idMauSac is not null and v.idKichThuoc is not null order by v.id",SanPhamChiTiet.class).getResultList();
        assertFalse(eligible.isEmpty(),"live catalogue must contain an eligible variant");
        var current=prices.getPrices(eligible,LocalDateTime.now());
        return eligible.stream().filter(v->!current.get(v.getId()).isDiscounted()).findFirst().orElseThrow();
    }

    private void campaign(SanPhamChiTiet variant, LocalDateTime begin, LocalDateTime end, boolean enabled, int status) {
        var campaign=new DotGiamGia();
        campaign.setMaDotGiamGia("ST"+UUID.randomUUID().toString().replace("-","").substring(0,12));
        campaign.setTenDotGiamGia("Storefront rollback test");
        campaign.setPhanTramGiamDot(new BigDecimal("99"));campaign.setNgayBatDau(begin);campaign.setNgayKetThuc(end);
        campaign.setKichHoat(enabled);campaign.setTrangThai(status);campaign.setNgayTao(LocalDateTime.now());
        em.persist(campaign);
        var link=new ChiTietDotGiamGia();link.setIdDotGiamGia(campaign);link.setIdSanPhamChiTiet(variant);
        link.setTrangThai(1);link.setNgayTao(LocalDateTime.now());em.persist(link);em.flush();
    }

    private StorefrontVariantResponse response(SanPhamChiTiet variant) {
        return store.detail(variant.getIdSanPham().getId()).variants().stream()
                .filter(v->v.id().equals(variant.getId())).findFirst().orElseThrow();
    }

    @Test void futureExpiredDisabledAndInactiveCampaignsNeverLeakIntoStorefrontPrice() {
        var variant=variant();var original=variant.getGiaBan();var now=LocalDateTime.now();
        campaign(variant,now.plusDays(1),now.plusDays(2),true,1);
        campaign(variant,now.minusDays(2),now.minusDays(1),true,1);
        campaign(variant,now.minusDays(1),now.plusDays(1),false,1);
        campaign(variant,now.minusDays(1),now.plusDays(1),true,0);
        var response=response(variant);
        assertFalse(response.dangGiamGia());assertEquals(0,response.phanTramGiam().signum());
        assertEquals(0,original.compareTo(response.giaSauGiam()));
        assertEquals(0,original.compareTo(variant.getGiaBan()));
    }

    @Test void saleExpiresByTimeWithoutUpdatingAnyProductOrCampaignRow() throws Exception {
        var variant=variant();var original=variant.getGiaBan();var deadline=LocalDateTime.now().plusSeconds(8);
        campaign(variant,LocalDateTime.now().minusDays(1),deadline,true,1);
        var active=response(variant);
        assertTrue(active.dangGiamGia());assertEquals(0,new BigDecimal("99").compareTo(active.phanTramGiam()));
        Thread.sleep(Math.max(0,Duration.between(LocalDateTime.now(),deadline).toMillis()+250));
        var expired=response(variant);
        assertFalse(expired.dangGiamGia());assertEquals(0,expired.phanTramGiam().signum());
        assertEquals(0,original.compareTo(expired.giaSauGiam()));
        assertEquals(0,original.compareTo(variant.getGiaBan()));
    }

    @Test void disabledVariantsAndInactiveProductsCannotBeReadByCustomers() {
        var variant=variant();var parent=variant.getIdSanPham();
        variant.setKichHoat(false);em.flush();
        var others=em.createQuery("select count(v) from SanPhamChiTiet v where v.idSanPham.id=:id"
                +" and v.trangThai=1 and v.kichHoat=true and v.giaBan>0",Long.class).setParameter("id",parent.getId()).getSingleResult();
        if(others>0) assertTrue(store.detail(parent.getId()).variants().stream().noneMatch(v->v.id().equals(variant.getId())));
        else assertThrows(AppException.class,()->store.detail(parent.getId()));
        parent.setTrangThai(0);em.flush();
        assertThrows(AppException.class,()->store.detail(parent.getId()));
        var query=new StorefrontQuery();query.setKeyword(parent.getMaSanPham());query.setSize(48);
        assertTrue(store.list(query).getContent().stream().noneMatch(p->p.id().equals(parent.getId())));
    }
}
