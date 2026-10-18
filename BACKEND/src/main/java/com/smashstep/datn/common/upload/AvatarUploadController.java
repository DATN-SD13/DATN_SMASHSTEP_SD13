package com.smashstep.datn.common.upload;

import com.smashstep.datn.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads/avatar")
@RequiredArgsConstructor
public class AvatarUploadController {
    private final AvatarUploadService service;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(Map.of("url", service.upload(file)));
    }
}
