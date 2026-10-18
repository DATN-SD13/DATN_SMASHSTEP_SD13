package com.smashstep.datn.common;

import com.smashstep.datn.common.exception.GlobalExceptionHandler;
import com.smashstep.datn.common.upload.AvatarUploadController;
import com.smashstep.datn.common.upload.AvatarUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvatarUploadTest {
    @TempDir Path directory;
    private AvatarUploadService service;
    private MockMvc mvc;
    @BeforeEach void setup() {
        service = new AvatarUploadService(directory.toString());
        mvc = MockMvcBuilders.standaloneSetup(new AvatarUploadController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void invalidMimeIsRejectedWithoutSaving() throws Exception {
        mvc.perform(multipart("/api/uploads/avatar").file(new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    @Test void overFiveMegabytesIsRejected() throws Exception {
        mvc.perform(multipart("/api/uploads/avatar").file(new MockMultipartFile("file", "a.png", "image/png", new byte[(int)AvatarUploadService.MAX_SIZE + 1])))
                .andExpect(status().isPayloadTooLarge());
    }
    @Test void spoofedMimeAndMissingFileAreRejected() throws Exception {
        mvc.perform(multipart("/api/uploads/avatar").file(new MockMultipartFile("file", "a.png", "image/png", "<script>bad</script>".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads/avatar")).andExpect(status().isBadRequest());
    }
    @Test void pngIsSavedUnderRandomNameWithoutTraversal() throws Exception {
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String url = service.upload(new MockMultipartFile("file", "../../other.png", "image/png", bytes.toByteArray()));
        assertTrue(url.matches("/uploads/avatars/[0-9a-f-]{36}\\.png"));
        Path stored = directory.resolve(url.substring(url.lastIndexOf('/') + 1));
        assertArrayEquals(bytes.toByteArray(), Files.readAllBytes(stored));
        assertEquals(directory, stored.getParent());
        mvc.perform(multipart("/api/uploads/avatar").file(new MockMultipartFile("file", "avatar.png", "image/png", bytes.toByteArray())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.url").exists());
    }
}
