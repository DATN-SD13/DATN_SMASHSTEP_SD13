package com.smashstep.datn.product;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.repository.*;
import com.smashstep.datn.product.service.HinhAnhSanPhamService;
import com.smashstep.datn.product.config.ThuMucAnhSanPham;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HinhAnhSanPhamUploadTest {
    @TempDir Path directory;
    SanPhamRepository products;
    HinhAnhSanPhamRepository images;
    HinhAnhSanPhamService service;

    @BeforeEach void setup() {
        products = mock(SanPhamRepository.class);
        images = mock(HinhAnhSanPhamRepository.class);
        service = new HinhAnhSanPhamService(products, images);
        ReflectionTestUtils.setField(service, "thuMucAnh", directory.toString());
        SanPham product = new SanPham(); product.setId(7L);
        when(products.timVaKhoaTheoId(7L)).thenReturn(Optional.of(product));
        when(images.findByIdSanPham_IdOrderByIdAsc(7L)).thenReturn(List.of());
        when(images.save(any())).thenAnswer(call -> {
            var image = call.getArgument(0, com.smashstep.datn.product.entity.HinhAnhSanPham.class);
            image.setId(1L); return image;
        });
    }

    private byte[] jpeg() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpg", output);
        return output.toByteArray();
    }

    @Test void resolvesRelativeStorageInsideBackendAndPreservesAbsoluteConfiguration() throws Exception {
        Path classes = Paths.get(ThuMucAnhSanPham.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path backend = classes.getParent().getParent();
        assertEquals(backend.resolve("uploads/products"), ThuMucAnhSanPham.resolve("uploads/products"));
        assertEquals(directory, ThuMucAnhSanPham.resolve(directory.toString()));
    }

    @Test void storesRealFileWithUuidAndOnlyWebPathInResponse() throws Exception {
        byte[] content = jpeg();
        var result = service.taiAnh(7L, new MockMultipartFile("file", "../../ảnh @# (1).jpg", "image/jpeg", content), false, null);
        assertTrue(result.getUrlAnh().matches("/uploads/products/[0-9a-f-]{36}\\.jpg"));
        assertEquals(7L, result.getSanPhamId()); assertTrue(result.getIsAnhChinh());
        Path file = directory.resolve(result.getUrlAnh().substring("/uploads/products/".length()));
        assertArrayEquals(content, Files.readAllBytes(file));
        assertEquals(directory, file.getParent());
    }

    @Test void rejectsInvalidEmptyOversizedAndDisguisedFilesBeforeSaving() throws Exception {
        for (String mime : List.of("application/pdf", "text/plain", "application/octet-stream", "image/svg+xml")) {
            assertThrows(AppException.class, () -> service.taiAnh(7L, new MockMultipartFile("file", "abc.jpg", mime, new byte[20]), false, null));
        }
        assertThrows(AppException.class, () -> service.taiAnh(7L, new MockMultipartFile("file", "empty.png", "image/png", new byte[0]), false, null));
        assertThrows(AppException.class, () -> service.taiAnh(7L, new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]), false, null));
        assertThrows(AppException.class, () -> service.taiAnh(7L, new MockMultipartFile("file", "fake.jpg", "image/jpeg", new byte[20]), false, null));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
        verify(images, never()).save(any());
    }

    @Test void rejectsMissingProductAndUnsupportedColor() throws Exception {
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg());
        assertThrows(AppException.class, () -> service.taiAnh(999L, file, false, null));
        assertThrows(AppException.class, () -> service.taiAnh(7L, file, false, 1L));
        verify(images, never()).save(any());
    }

    @Test void removesFileIfDatabaseSaveFails() throws Exception {
        doThrow(new IllegalStateException("Database unavailable")).when(images).save(any());
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg());
        assertThrows(IllegalStateException.class, () -> service.taiAnh(7L, file, false, null));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }

    @Test void removesFileOnTransactionRollback() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            var result = service.taiAnh(7L, new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg()), false, null);
            Path file = directory.resolve(result.getUrlAnh().substring("/uploads/products/".length()));
            assertTrue(Files.exists(file));
            for (var synchronization : TransactionSynchronizationManager.getSynchronizations())
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            assertFalse(Files.exists(file));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
