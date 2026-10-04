package com.smashstep.datn.product;

import com.smashstep.datn.product.dto.ProductDtos.*;
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

class ProductServiceTest {
    private final SanPhamRepository products = mock(SanPhamRepository.class);
    private final SanPhamChiTietRepository variants = mock(SanPhamChiTietRepository.class);
    private final HinhAnhSanPhamRepository images = mock(HinhAnhSanPhamRepository.class);
    private ProductService service;
    private SanPham product;

    @BeforeEach
    void setup() {
        service = new ProductService(products, variants, images, mock(DanhMucRepository.class),
                mock(ThuongHieuRepository.class), mock(ChatLieuRepository.class), mock(KieuDangRepository.class),
                mock(CoGiayRepository.class), mock(XuatXuRepository.class));
        product = new SanPham(); product.setId(1L); product.setMaSanPham("SP001"); product.setTrangThai(1);
    }
    private ProductRequest request(String code) {
        return new ProductRequest(code, "Tên giày", 1L, 1L, 1L, 1L, 1L, 1L, "", 1);
    }

    @Test
    void duplicateCodeReturnsConflictWithoutWriting() {
        when(products.existsByMaSanPhamIgnoreCase("SP001")).thenReturn(true);
        var ex = assertThrows(ProductException.class, () -> service.create(request("SP001")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(products, never()).save(any());
    }

    @Test
    void unknownAttributeReturnsBadRequestWithoutWriting() {
        var ex = assertThrows(ProductException.class, () -> service.create(request("SP002")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(products, never()).save(any());
    }

    @Test
    void refusesChangingProductCode() {
        when(products.findLockedById(1L)).thenReturn(Optional.of(product));
        assertThrows(ProductException.class, () -> service.update(1L, request("SP002")));
        verify(products, never()).save(any());
    }

    @Test
    void statusUpdateKeepsVariantsAndSetsTimestamp() {
        when(products.findLockedById(1L)).thenReturn(Optional.of(product));
        when(products.save(product)).thenReturn(product);
        when(variants.summarize(List.of(1L))).thenReturn(List.of());
        service.changeStatus(1L, 0);
        assertEquals(0, product.getTrangThai());
        assertNotNull(product.getNgayCapNhat());
        verify(variants, never()).save(any());
        verify(products, never()).delete(any(SanPham.class));
    }

    @Test
    void selectingNewMainImageClearsPreviousMain() {
        when(products.findLockedById(1L)).thenReturn(Optional.of(product));
        var previous = new HinhAnhSanPham(); previous.setId(10L); previous.setIsAnhChinh(true);
        when(images.findByIdSanPham_IdOrderByIdAsc(1L)).thenReturn(List.of(previous));
        when(images.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.addImage(1L, new ImageRequest("https://example.com/shoe.png", true));
        assertTrue(result.isAnhChinh());
        assertFalse(previous.getIsAnhChinh());
    }

    @Test
    void rejectsNonHttpImageAndReturns404ForUnknownProduct() {
        when(products.findLockedById(1L)).thenReturn(Optional.of(product));
        assertThrows(ProductException.class, () -> service.addImage(1L, new ImageRequest("javascript:alert(1)", true)));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ProductException.class, () -> service.detail(99L)).getStatus());
        verify(images, never()).save(any());
    }
}
