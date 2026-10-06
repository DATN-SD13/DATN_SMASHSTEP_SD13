package com.smashstep.datn.promotion;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.product.repository.SanPhamChiTietRepository;
import com.smashstep.datn.promotion.entity.DotGiamGia;
import com.smashstep.datn.promotion.repository.ChiTietDotGiamGiaRepository;
import com.smashstep.datn.promotion.repository.DotGiamGiaRepository;
import com.smashstep.datn.promotion.request.DotGiamGiaRequest;
import com.smashstep.datn.promotion.service.DotGiamGiaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DotGiamGiaServiceTest {
    @Test void inactiveDisabledOrInactiveParentVariantAndDuplicateIdsAreRejected() {
        var product = new SanPham(); product.setTrangThai(1);
        var variant = new SanPhamChiTiet(); variant.setId(1L); variant.setIdSanPham(product); variant.setKichHoat(true); variant.setTrangThai(0);
        when(variants.findById(1L)).thenReturn(Optional.of(variant));
        var request = new DotGiamGiaRequest(); request.setName("Eligibility"); request.setDiscountValue(BigDecimal.TEN);
        request.setStartDate(LocalDate.of(2097, 1, 1)); request.setEndDate(LocalDate.of(2097, 1, 31)); request.setProductDetailIds(List.of(1L));
        assertThrows(AppException.class, () -> service.create(request));
        variant.setTrangThai(1); variant.setKichHoat(false); assertThrows(AppException.class, () -> service.create(request));
        variant.setKichHoat(true); product.setTrangThai(0); assertThrows(AppException.class, () -> service.create(request));
        product.setTrangThai(1); request.setProductDetailIds(List.of(1L, 1L)); assertThrows(AppException.class, () -> service.create(request));
        verify(campaigns, never()).save(any());
    }

    @Test void overlappingDatesForSameVariantAreConflict() {
        var product = new SanPham(); product.setTrangThai(1);
        var variant = new SanPhamChiTiet(); variant.setId(1L); variant.setIdSanPham(product); variant.setKichHoat(true); variant.setTrangThai(1);
        when(variants.findById(1L)).thenReturn(Optional.of(variant));
        var existing = new DotGiamGia(); existing.setId(10L); existing.setMaDotGiamGia("DGG010");
        existing.setNgayBatDau(LocalDate.of(2097, 10, 1).atStartOfDay()); existing.setNgayKetThuc(LocalDate.of(2097, 10, 31).atTime(23,59,59));
        var link = new com.smashstep.datn.promotion.entity.ChiTietDotGiamGia(); link.setIdDotGiamGia(existing);
        when(details.findActiveByProductDetailId(1L)).thenReturn(List.of(link));
        var request = new DotGiamGiaRequest(); request.setName("Overlap"); request.setDiscountValue(BigDecimal.TEN);
        request.setStartDate(LocalDate.of(2097, 10, 15)); request.setEndDate(LocalDate.of(2097, 11, 15)); request.setProductDetailIds(List.of(1L));
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.create(request)).getStatus());
        verify(campaigns, never()).save(any());
    }
    private final DotGiamGiaRepository campaigns = mock(DotGiamGiaRepository.class);
    private final ChiTietDotGiamGiaRepository details = mock(ChiTietDotGiamGiaRepository.class);
    private final SanPhamChiTietRepository variants = mock(SanPhamChiTietRepository.class);
    private final DotGiamGiaService service = new DotGiamGiaService(campaigns, details, variants, mock(com.smashstep.datn.common.config.DatabaseCapabilities.class));

    @Test
    void createsCodeFromMaximumValidCodeInsteadOfLastIdentity() {
        when(campaigns.findAllCodes()).thenReturn(Arrays.asList("DGG004", "DGG999", "DGG007", "invalid", null));
        when(campaigns.save(any())).thenAnswer(call -> {
            DotGiamGia campaign = call.getArgument(0);
            campaign.setId(50L);
            return campaign;
        });
        var variant = new SanPhamChiTiet();
        variant.setId(1L);
        variant.setTrangThai(1); variant.setKichHoat(true);
        var activeProduct = new SanPham(); activeProduct.setTrangThai(1); variant.setIdSanPham(activeProduct);
        when(variants.findById(1L)).thenReturn(Optional.of(variant));
        var request = new DotGiamGiaRequest();
        request.setName("Giảm giá demo");
        request.setDiscountValue(BigDecimal.TEN);
        request.setStartDate(LocalDate.of(2026, 10, 1));
        request.setEndDate(LocalDate.of(2026, 10, 31));
        request.setProductDetailIds(List.of(1L));

        assertEquals("DGG1000", service.create(request).getCode());
    }

    @Test
    void listClampsInvalidSizeAndUsesOneBasedPage() {
        when(campaigns.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        service.getAll(null, null, null, null, -1, 0);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(campaigns).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void nullDatesProduceSafeDetailAndCannotActivate() {
        var campaign = new DotGiamGia();
        campaign.setId(2L);
        campaign.setTrangThai(1);
        when(campaigns.findByMaDotGiamGia("DGG002")).thenReturn(Optional.of(campaign));

        assertEquals("KHONG_XAC_DINH", service.getByMa("DGG002").getTimeStatus());
        var error = assertThrows(AppException.class, () -> service.updateStatus("DGG002", 1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(campaigns, never()).save(any());
    }

    @Test
    void variantSearchAcceptsProductWithNullNameAndCode() {
        var product = new SanPham();
        var variant = new SanPhamChiTiet();
        variant.setId(1L);
        product.setTrangThai(1); variant.setKichHoat(true);
        variant.setIdSanPham(product);
        variant.setTrangThai(1);
        variant.setSku("SHOE-001");
        when(variants.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(variant)), Page.empty());

        assertEquals(1, service.getProductDetails("shoe").size());
        assertEquals(0, service.getProductDetails("missing").size());
    }
}
