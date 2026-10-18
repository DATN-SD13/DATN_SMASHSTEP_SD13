package com.smashstep.datn.product;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.dto.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SanPhamServiceTest {
    private final SanPhamRepository products = mock(SanPhamRepository.class);
    private final SanPhamChiTietRepository variants = mock(SanPhamChiTietRepository.class);
    private final HinhAnhSanPhamRepository images = mock(HinhAnhSanPhamRepository.class);
    private SanPhamService service;
    private HinhAnhSanPhamService imageService;
    private SanPham product;

    @BeforeEach
    void setup() {
        service = new SanPhamService(mock(KhoaGhiSanPham.class), mock(com.smashstep.datn.common.pricing.CurrentPriceService.class), products, variants, images, mock(DanhMucRepository.class),
                mock(ThuongHieuRepository.class), mock(ChatLieuRepository.class), mock(KieuDangRepository.class),
                mock(CoGiayRepository.class), mock(XuatXuRepository.class));
        imageService = new HinhAnhSanPhamService(products, images);
        product = new SanPham(); product.setId(1L); product.setMaSanPham("SP001"); product.setTrangThai(1);
    }
    private SanPhamThemRequest request(String code) {
        return new SanPhamThemRequest(code, "Tên giày", 1L, 1L, 1L, 1L, 1L, 1L, "", 1);
    }

    @Test
    void duplicateCodeReturnsConflictWithoutWriting() {
        when(products.existsByMaSanPhamIgnoreCase("SP001")).thenReturn(true);
        var ex = assertThrows(AppException.class, () -> service.themSanPham(request("SP001")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(products, never()).save(any());
    }

    @Test
    void unknownAttributeReturnsNotFoundWithoutWriting() {
        var ex = assertThrows(AppException.class, () -> service.themSanPham(request("SP002")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(products, never()).save(any());
    }

    @Test
    void refusesChangingProductCode() {
        when(products.timVaKhoaTheoId(1L)).thenReturn(Optional.of(product));
        assertThrows(AppException.class, () -> service.suaSanPham(1L, new SanPhamSuaRequest("SP002", "Tên giày", 1L, 1L, 1L, 1L, 1L, 1L, "", 1)));
        verify(products, never()).save(any());
    }

    @Test
    void statusUpdateKeepsVariantsAndSetsTimestamp() {
        when(products.timVaKhoaTheoId(1L)).thenReturn(Optional.of(product));
        when(products.save(product)).thenReturn(product);
        when(variants.tongHopTheoSanPham(List.of(1L))).thenReturn(List.of());
        service.doiTrangThai(1L, 0);
        assertEquals(0, product.getTrangThai());
        assertNotNull(product.getNgayCapNhat());
        verify(variants, never()).save(any());
        verify(products, never()).delete(any(SanPham.class));
    }

    @Test
    void selectingNewMainImageClearsPreviousMain() {
        when(products.timVaKhoaTheoId(1L)).thenReturn(Optional.of(product));
        var previous = new HinhAnhSanPham(); previous.setId(10L); previous.setIsAnhChinh(true);
        when(images.findByIdSanPham_IdOrderByIdAsc(1L)).thenReturn(List.of(previous));
        when(images.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = imageService.themAnh(1L, new HinhAnhSanPhamRequest("https://example.com/shoe.png", true));
        assertTrue(result.getIsAnhChinh());
        assertFalse(previous.getIsAnhChinh());
    }

    @Test
    void rejectsNonHttpImageAndReturns404ForUnknownProduct() {
        when(products.timVaKhoaTheoId(1L)).thenReturn(Optional.of(product));
        assertThrows(AppException.class, () -> imageService.themAnh(1L, new HinhAnhSanPhamRequest("javascript:alert(1)", true)));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.layChiTietSanPham(99L)).getStatus());
        verify(images, never()).save(any());
    }
}
