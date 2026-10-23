package com.smashstep.datn.adminai.controller;

import com.smashstep.datn.adminai.dto.AdminAiDto.AskRequest;
import com.smashstep.datn.adminai.dto.AdminAiDto.Reply;
import com.smashstep.datn.adminai.service.AdminAiService;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AdminAiService adminAiService;

    @PostMapping("/ask")
    public ApiResponse<Reply> ask(@Valid @RequestBody AskRequest request) {
        return ApiResponse.ok("Trợ lý quản trị đã phân tích dữ liệu", adminAiService.ask(request.message()));
    }
}
