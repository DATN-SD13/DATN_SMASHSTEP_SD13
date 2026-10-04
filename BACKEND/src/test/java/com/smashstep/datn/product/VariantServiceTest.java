package com.smashstep.datn.product;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VariantServiceTest {
    private final SanPhamChiTietRepository variants = mock(SanPhamChiTietRepository.class);
    private final SanPhamRepository products = mock(SanPhamRepository.class);
    private final MauSacRepository colors = mock(MauSacRepository.class);
    private final KichThuocRepository sizes = mock(KichThuocRepository.class);
    private final VariantService service = new VariantService(variants, products, colors, sizes);
    private SanPham product;

    @BeforeEach
    void setup() {
        product = new SanPham(); product.setId(1L); product.setMaSanPham("SP001");
        when(products.findLockedById(1L)).thenReturn(Optional.of(product));
    }
    private VariantRequest request(String code, String sku, Long sizeId) {
        return new VariantRequest(1L, code, sku, 1L, sizeId, 2, new BigDecimal("1200000"), true, 1);
    }

    @Test
    void refusesExistingCodeSkuAndCombination() {
        when(variants.existsByMaChiTietSanPhamIgnoreCaseAndIdNot("CT001", 0L)).thenReturn(true);
        assertThrows(ProductException.class, () -> service.create(request("CT001", "SKU1", 1L)));
        when(variants.existsBySkuIgnoreCaseAndIdNot("SKU2", 0L)).thenReturn(true);
        assertThrows(ProductException.class, () -> service.create(request("CT002", "SKU2", 1L)));
        when(variants.existsByIdSanPham_IdAndIdMauSac_IdAndIdKichThuoc_IdAndIdNot(1L, 1L, 1L, 0L)).thenReturn(true);
        assertThrows(ProductException.class, () -> service.create(request("CT003", "SKU3", 1L)));
        verify(variants, never()).save(any());
    }

    @Test
    void detectsRepeatedBatchCombinationBeforeAnyWrite() {
        assertThrows(ProductException.class, () -> service.createBatch(1L,
                List.of(request("CT1", "SKU1", 1L), request("CT2", "SKU2", 1L))));
        verify(variants, never()).save(any());
    }

    @Test
    void detectsCaseInsensitiveBatchSkuBeforeAnyWrite() {
        assertThrows(ProductException.class, () -> service.createBatch(1L,
                List.of(request("CT1", "SameSku", 1L), request("CT2", "samesku", 2L))));
        verify(variants, never()).save(any());
    }

    @Test
    void refusesMovingAnExistingVariantToAnotherProduct() {
        SanPhamChiTiet variant = new SanPhamChiTiet(); variant.setId(9L); variant.setIdSanPham(product);
        when(variants.findById(9L)).thenReturn(Optional.of(variant));
        var request = new VariantRequest(2L, "CT1", "SKU1", 1L, 1L, 2, BigDecimal.ONE, true, 1);
        assertThrows(ProductException.class, () -> service.update(9L, request));
        verify(variants, never()).save(any());
    }

    @Test
    void refusesUnknownOrInactiveColorAndCreatesValidVariant() {
        assertThrows(ProductException.class, () -> service.create(request("CT1", "SKU1", 1L)));
        MauSac color = new MauSac(); color.setId(1L); color.setTrangThai(0);
        KichThuoc size = new KichThuoc(); size.setId(1L); size.setTrangThai(1);
        when(colors.findById(1L)).thenReturn(Optional.of(color)); when(sizes.findById(1L)).thenReturn(Optional.of(size));
        assertThrows(ProductException.class, () -> service.create(request("CT1", "SKU1", 1L)));
        color.setTrangThai(1);
        when(variants.save(any())).thenAnswer(i -> { SanPhamChiTiet v = i.getArgument(0); v.setId(10L); return v; });
        var response = service.create(request("CT1", "SKU1", 1L));
        assertEquals(10L, response.id()); assertEquals(1L, response.sanPhamId());
        assertEquals(new BigDecimal("1200000"), response.giaBan());
    }
}

