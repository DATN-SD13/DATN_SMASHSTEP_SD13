package com.smashstep.datn.product;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.DuLieuSanPham.*;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThuocTinhSanPhamServiceTest {
    private final DanhMucRepository categories = mock(DanhMucRepository.class);
    private final ThuongHieuRepository brands = mock(ThuongHieuRepository.class);
    private final ChatLieuRepository materials = mock(ChatLieuRepository.class);
    private final XuatXuRepository origins = mock(XuatXuRepository.class);
    private final CoGiayRepository collars = mock(CoGiayRepository.class);
    private final KieuDangRepository styles = mock(KieuDangRepository.class);
    private final MauSacRepository colors = mock(MauSacRepository.class);
    private final KichThuocRepository sizes = mock(KichThuocRepository.class);
    private final ThuocTinhSanPhamService service = new ThuocTinhSanPhamService(categories, brands,
            materials, origins, collars, styles, colors, sizes);

    private void codes(String type, List<String> codes) {
        switch (type) {
            case "categories" -> when(categories.layDanhSachMa()).thenReturn(codes);
            case "brands" -> when(brands.layDanhSachMa()).thenReturn(codes);
            case "materials" -> when(materials.layDanhSachMa()).thenReturn(codes);
            case "styles" -> when(styles.layDanhSachMa()).thenReturn(codes);
            case "collars" -> when(collars.layDanhSachMa()).thenReturn(codes);
            case "origins" -> when(origins.layDanhSachMa()).thenReturn(codes);
            case "colors" -> when(colors.layDanhSachMa()).thenReturn(codes);
            default -> fail("Loại thuộc tính không hợp lệ");
        }
    }

    @ParameterizedTest
    @CsvSource({ "categories,DM,008,DM009", "brands,TH,010,TH011", "materials,CL,010,CL011",
            "styles,KD,010,KD011", "collars,CG,003,CG004", "origins,XX,008,XX009", "colors,MS,015,MS016" })
    void generatesCorrectPrefixFromLargestNumericSuffix(String type, String prefix, String max, String expected) {
        codes(type, List.of(prefix + max, prefix + "001", prefix + "002"));
        assertEquals(expected, service.taoMaTiepTheo(type));
    }

    @Test
    void ignoresDirtyCodesAndUsesMaxPlusOneWithoutFillingGaps() {
        codes("colors", Arrays.asList("MS001", "MSABC", "TEST", null, "", "MS010", "XX999", "MS-20", "MS12x"));
        assertEquals("MS011", service.taoMaTiepTheo("colors"));
        codes("categories", List.of("DM001", "DM002", "DM005"));
        assertEquals("DM006", service.taoMaTiepTheo("categories"));
    }

    @Test
    void supportsEmptyDatabaseMoreThan999AndNumbersBeyondLong() {
        codes("colors", List.of());
        assertEquals("MS001", service.taoMaTiepTheo("colors"));
        codes("colors", List.of("MS999"));
        assertEquals("MS1000", service.taoMaTiepTheo("colors"));
        codes("colors", List.of("MS9223372036854775807"));
        assertEquals("MS9223372036854775808", service.taoMaTiepTheo("colors"));
        codes("colors", List.of("MS" + "9".repeat(48)));
        assertThrows(AppException.class, () -> service.taoMaTiepTheo("colors"));
    }

    @Test
    void createNeedsNoClientCodeAndIgnoresForgedCode() {
        codes("brands", List.of("TH001", "TH010"));
        when(brands.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("TH011", service.luuThuocTinh("brands", null,
                new ThuocTinhRequest(null, "Thương hiệu mới", "", null, 1)).getMa());
        assertEquals("TH011", service.luuThuocTinh("brands", null,
                new ThuocTinhRequest("FORGED999", "Tên khác", "", null, 1)).getMa());
    }

    @Test
    void updateKeepsCodeIndependentOfIdentityAndNeverCallsNextCode() {
        MauSac color = new MauSac();
        color.setId(120L); color.setMaMauSac("MS015"); color.setTenMauSac("Xanh biển");
        when(colors.findById(120L)).thenReturn(Optional.of(color));
        when(colors.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.luuThuocTinh("colors", 120L,
                new ThuocTinhRequest("MS999", "Xanh navy", "", "#123456", 1));
        assertEquals("MS015", result.getMa()); assertEquals("Xanh navy", result.getTen());
        verify(colors, never()).layDanhSachMa();
    }

    @Test
    void sizeHasNoCodeAndNextCodeIsRejected() {
        AppException error = assertThrows(AppException.class, () -> service.taoMaTiepTheo("sizes"));
        assertEquals("Kích thước không có mã.", error.getMessage());
        when(sizes.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.luuThuocTinh("sizes", null, new ThuocTinhRequest(null, "46.5", "Ghi chú", null, 1));
        assertNull(result.getMa()); assertEquals("46.5", result.getTen());
        verifyNoInteractions(categories, brands, materials, origins, collars, styles, colors);
    }

    @Test
    void rejectsInvalidHex() {
        assertThrows(AppException.class, () -> service.luuThuocTinh("colors", null, new ThuocTinhRequest("RED", "Đỏ", "", "red", 1)));
    }

    @Test
    void concurrentDuplicateFromUniqueIndexReturns409InsteadOf500() throws Exception {
        codes("brands", List.of("TH010"));
        when(brands.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate code"));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new com.smashstep.datn.product.controller.ThuocTinhSanPhamController(service))
                .setControllerAdvice(new com.smashstep.datn.common.exception.GlobalExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/product-attributes/brands")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"ten\":\"Thương hiệu mới\",\"trangThai\":1}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void rejectsUnknownTypeAndTooLongSize() {
        assertThrows(AppException.class, () -> service.layDanhSach("unknown", 0, 10, "", null));
        assertThrows(AppException.class, () -> service.luuThuocTinh("sizes", null, new ThuocTinhRequest(null, "x".repeat(51), "", null, 1)));
    }
}
