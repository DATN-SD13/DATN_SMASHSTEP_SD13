package com.smashstep.datn.storefront;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.storefront.dto.StorefrontQuery;
import com.smashstep.datn.storefront.service.StorefrontProductService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reads the real canonical SQL Server only. No fixture insert/update/delete or schema changes. */
@SpringBootTest(classes=DatnApplication.class, properties={
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.smashstep.datn.storefront.StorefrontReadOnlyIntegrationTest$SqlCapture"
})
@Transactional(readOnly=true)
public class StorefrontReadOnlyIntegrationTest {
    public static class SqlCapture implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final java.util.List<String> statements = new java.util.concurrent.CopyOnWriteArrayList<>();
        public String inspect(String sql) { statements.add(sql); return sql; }
    }
    @Autowired StorefrontProductService store;
    @Autowired CurrentPriceService prices;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    MockMvc mvc;

    @BeforeEach void setup() { mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }

    @Test void publicReadsSucceedAndAdminWritesRemainProtected() throws Exception {
        for(String path:List.of("/api/store/products","/api/store/filters","/api/store/brands"))
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        for(String path:List.of("/api/store/products","/api/products")) {
            int status=mvc.perform(post(path).contentType("application/json").content("{}")).andReturn().getResponse().getStatus();
            assertTrue(status==401 || status==403, path+" must reject anonymous writes");
        }
        int admin=mvc.perform(get("/api/products")).andReturn().getResponse().getStatus();
        assertTrue(admin==401 || admin==403, "admin catalogue remains authenticated");
    }

    @Test void invalidAndInactiveProductsReturnClean404() throws Exception {
        mvc.perform(get("/api/store/products/"+Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        var inactive=em.createQuery("select p.id from SanPham p where p.trangThai <> 1",Long.class).setMaxResults(1).getResultList();
        for(Long id:inactive) assertEquals(HttpStatus.NOT_FOUND,assertThrows(AppException.class,()->store.detail(id)).getStatus());
    }

    @Test void listAndDetailUseCanonicalVariantPricesAndEligibility() {
        StorefrontQuery query=new StorefrontQuery(); query.setSize(48);
        var page=store.list(query);
        assertTrue(page.getContent().size()<=48);
        for(var product:page.getContent()) {
            assertEquals(1,em.find(SanPham.class,product.id()).getTrangThai());
            var detail=store.detail(product.id());
            assertFalse(detail.variants().isEmpty());
            for(var variant:detail.variants()) {
                var entity=em.find(SanPhamChiTiet.class,variant.id());
                assertEquals(1,entity.getTrangThai()); assertEquals(Boolean.TRUE,entity.getKichHoat());
                var price=prices.getPrice(entity,LocalDateTime.now());
                assertEquals(0,price.getOriginalPrice().compareTo(variant.giaGoc()));
                assertEquals(0,price.getEffectivePrice().compareTo(variant.giaSauGiam()));
                assertEquals(0,price.getDiscountPercent().compareTo(variant.phanTramGiam()));
                assertEquals(price.isDiscounted(),variant.dangGiamGia());
            }
            var representative=detail.variants().stream().filter(v->v.giaSauGiam().compareTo(product.giaSauGiam())==0
                    && v.giaGoc().compareTo(product.giaGoc())==0 && v.phanTramGiam().compareTo(product.phanTramGiam())==0).toList();
            assertFalse(representative.isEmpty(),"card price/badge must belong to the same real eligible variant");
            assertEquals(detail.variants().stream().mapToLong(v->v.soLuong()).sum(),product.soLuong());
        }
    }

    @Test void sortingPaginationAndLiteralSearchAreReal() {
        StorefrontQuery q=new StorefrontQuery(); q.setSize(48); q.setSort("priceAsc");
        var ascending=store.list(q);
        for(int i=1;i<ascending.getContent().size();i++)
            assertTrue(ascending.getContent().get(i-1).giaSauGiam().compareTo(ascending.getContent().get(i).giaSauGiam())<=0);
        q.setSort("priceDesc"); var descending=store.list(q);
        for(int i=1;i<descending.getContent().size();i++)
            assertTrue(descending.getContent().get(i-1).giaSauGiam().compareTo(descending.getContent().get(i).giaSauGiam())>=0);
        if(!ascending.getContent().isEmpty()) {
            var product=ascending.getContent().get(0); q.setKeyword(product.maSanPham());
            assertTrue(store.list(q).getContent().stream().anyMatch(p->p.id().equals(product.id())));
        }
        q.setKeyword("STORE_NONEXISTENT_"+System.nanoTime()); assertEquals(0,store.list(q).getTotalElements());
        q.setKeyword("%_["); assertNotNull(store.list(q));
        q.setKeyword(""); q.setPage(Integer.MAX_VALUE/48); assertTrue(store.list(q).getContent().isEmpty());
    }

    @Test void filtersMatchOneVariantAndCurrentPrice() {
        var all=store.list(new StorefrontQuery());
        var filters=store.filters();
        assertEquals(store.brands(),filters.brands());
        if(all.getContent().isEmpty()) { assertTrue(filters.brands().isEmpty()); return; }
        var product=all.getContent().get(0);
        var variant=store.detail(product.id()).variants().get(0);
        var q=new StorefrontQuery(); q.setBrandId(product.brandId()==null?null:List.of(product.brandId()));
        q.setColorId(List.of(variant.mauSacId())); q.setSizeId(List.of(variant.kichThuocId()));
        q.setMinPrice(variant.giaSauGiam()); q.setMaxPrice(variant.giaSauGiam());
        var matched=store.list(q);
        assertTrue(matched.getContent().stream().anyMatch(p->p.id().equals(product.id())));
        for(var candidate:matched.getContent()) assertTrue(store.detail(candidate.id()).variants().stream()
                .anyMatch(v->v.mauSacId().equals(variant.mauSacId()) && v.kichThuocId().equals(variant.kichThuocId())
                && v.giaSauGiam().compareTo(variant.giaSauGiam())==0));
    }

    @Test void malformedPaginationSortAndPricesAreRejected() throws Exception {
        for(String query:List.of("page=-1","size=0","size=1000","sort=unknown","minPrice=-1","minPrice=100&maxPrice=1","brandId=-1"))
            mvc.perform(get("/api/store/products?"+query)).andExpect(status().isBadRequest());
        var q=new StorefrontQuery(); q.setMinPrice(new BigDecimal("-1"));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(AppException.class,()->store.list(q)).getStatus());
    }

    @Test void catalogueImagesAndPricingUseBoundedQueriesInsteadOfPerCardQueries() {
        var q=new StorefrontQuery();q.setSize(1);
        em.clear();SqlCapture.statements.clear();store.list(q);int one=SqlCapture.statements.size();
        q.setSize(12);em.clear();SqlCapture.statements.clear();store.list(q);int twelve=SqlCapture.statements.size();
        assertTrue(one>0);
        assertTrue(twelve<=one+1,"query count must not grow per product card: "+one+" vs "+twelve);
    }
}
