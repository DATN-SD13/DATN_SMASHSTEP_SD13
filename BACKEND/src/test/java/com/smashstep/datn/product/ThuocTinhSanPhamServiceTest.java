package com.smashstep.datn.product;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThuocTinhSanPhamServiceTest {
    private final ThuocTinhSanPhamService service = new ThuocTinhSanPhamService(mock(DanhMucRepository.class),
            mock(ThuongHieuRepository.class), mock(ChatLieuRepository.class), mock(XuatXuRepository.class),
            mock(CoGiayRepository.class), mock(KieuDangRepository.class), mock(MauSacRepository.class), mock(KichThuocRepository.class));

    @Test
    void rejectsMissingAttributeCodeAndInvalidHex() {
        assertThrows(AppException.class, () -> service.save("brands", null, new ThuocTinhRequest("", "Nike", "", null, 1)));
        assertThrows(AppException.class, () -> service.save("colors", null, new ThuocTinhRequest("RED", "Đỏ", "", "red", 1)));
    }

    @Test
    void rejectsUnknownTypeAndTooLongSize() {
        assertThrows(AppException.class, () -> service.list("unknown", 0, 10, "", null));
        assertThrows(AppException.class, () -> service.save("sizes", null, new ThuocTinhRequest(null, "x".repeat(51), "", null, 1)));
    }
}
