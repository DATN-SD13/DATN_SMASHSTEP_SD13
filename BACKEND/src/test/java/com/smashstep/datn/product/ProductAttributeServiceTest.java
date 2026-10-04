package com.smashstep.datn.product;

import com.smashstep.datn.product.dto.ProductDtos.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductAttributeServiceTest {
    private final ProductAttributeService service = new ProductAttributeService(mock(DanhMucRepository.class),
            mock(ThuongHieuRepository.class), mock(ChatLieuRepository.class), mock(XuatXuRepository.class),
            mock(CoGiayRepository.class), mock(KieuDangRepository.class), mock(MauSacRepository.class), mock(KichThuocRepository.class));

    @Test
    void rejectsMissingAttributeCodeAndInvalidHex() {
        assertThrows(ProductException.class, () -> service.save("brands", null, new AttributeRequest("", "Nike", "", null, 1)));
        assertThrows(ProductException.class, () -> service.save("colors", null, new AttributeRequest("RED", "Đỏ", "", "red", 1)));
    }

    @Test
    void rejectsUnknownTypeAndTooLongSize() {
        assertThrows(ProductException.class, () -> service.list("unknown", 0, 10, "", null));
        assertThrows(ProductException.class, () -> service.save("sizes", null, new AttributeRequest(null, "x".repeat(51), "", null, 1)));
    }
}

