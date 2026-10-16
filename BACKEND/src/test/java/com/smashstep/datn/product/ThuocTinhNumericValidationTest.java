package com.smashstep.datn.product;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.exception.GlobalExceptionHandler;
import com.smashstep.datn.product.controller.ThuocTinhSanPhamController;
import com.smashstep.datn.product.dto.DuLieuSanPham.ThuocTinhRequest;
import com.smashstep.datn.product.dto.ThuocTinhTrungRequest;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

// Real controller/service, mocked repositories: no live database writes.
class ThuocTinhNumericValidationTest {
    final KichThuocRepository sizes = mock(KichThuocRepository.class);
    final MauSacRepository colors = mock(MauSacRepository.class);
    final ThuocTinhSanPhamService service = new ThuocTinhSanPhamService(mock(KhoaGhiSanPham.class),
            mock(DanhMucRepository.class), mock(ThuongHieuRepository.class), mock(ChatLieuRepository.class),
            mock(XuatXuRepository.class), mock(CoGiayRepository.class), mock(KieuDangRepository.class), colors, sizes);
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ThuocTinhSanPhamController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @ParameterizedTest @ValueSource(strings={"abc", "40abc", "size 40", "40#", "40..5", "40,,5", "-40", "0", "0.00", ".5", "40.", "1e2", "NaN"})
    void invalidSizesRejectedByCreateUpdateAndDuplicateApi(String value) throws Exception {
        String body = "{\"ten\":\"" + value + "\",\"trangThai\":1}";
        mvc.perform(post("/api/product-attributes/sizes").contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/product-attributes/sizes/1").contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/product-attributes/sizes/check-duplicate").contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verify(sizes, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"36", "40.5", "40,5", "40.0", " 41.5 "})
    void acceptsPositiveNumericStringsWithoutLosingPrecision(String value) throws Exception {
        when(sizes.save(any())).thenAnswer(i -> i.getArgument(0));
        mvc.perform(post("/api/product-attributes/sizes").contentType(APPLICATION_JSON)
                        .content("{\"giaTri\":\"" + value + "\",\"trangThai\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ten").value(value.trim().replace(',', '.')));
    }

    @Test void commaDuplicateAndStringIdentityPreserved() {
        KichThuoc size = new KichThuoc(); size.setId(1L); size.setGiaTri("40.5"); size.setTrangThai(1);
        when(sizes.findAll(org.mockito.ArgumentMatchers.<Specification<KichThuoc>>any())).thenReturn(List.of(size));
        assertTrue(service.kiemTraTrung("sizes", new ThuocTinhTrungRequest("40,5", null, null)).isDuplicate());
        size.setGiaTri("40");
        assertFalse(service.kiemTraTrung("sizes", new ThuocTinhTrungRequest("40.0", null, null)).isDuplicate());
    }

    @Test void colorCreateEditAndHexDuplicateUseExistingRules() {
        when(colors.layDanhSachMa()).thenReturn(List.of("MS001"));
        when(colors.save(any())).thenAnswer(i -> i.getArgument(0));
        var created = service.luuThuocTinh("colors", null, new ThuocTinhRequest(null,"Xanh",null,"#2879e3",1));
        assertEquals("#2879E3", created.getMaMauHex()); assertEquals("MS002", created.getMa());
        MauSac existing = new MauSac(); existing.setId(10L); existing.setMaMauSac("MS001"); existing.setTenMauSac("Cũ"); existing.setMaMauHex("#000000");
        when(colors.findById(10L)).thenReturn(Optional.of(existing));
        var updated = service.luuThuocTinh("colors",10L,new ThuocTinhRequest(null,"Mới",null,"#112233",1));
        assertEquals("#112233",updated.getMaMauHex()); assertEquals("MS001",updated.getMa());
        when(colors.findAll(org.mockito.ArgumentMatchers.<Specification<MauSac>>any())).thenReturn(List.of(existing));
        assertEquals("HEX",service.kiemTraTrung("colors",new ThuocTinhTrungRequest("Tên khác","#112233",null)).getReason());
        assertThrows(AppException.class, () -> service.luuThuocTinh("colors",null,new ThuocTinhRequest(null,"Trùng",null,"#112233",1)));
    }
}
