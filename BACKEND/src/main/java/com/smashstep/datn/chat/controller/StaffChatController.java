package com.smashstep.datn.chat.controller;

import com.smashstep.datn.chat.dto.ChatDto.SendMessageRequest;
import com.smashstep.datn.chat.dto.ChatDto.SessionDetail;
import com.smashstep.datn.chat.dto.ChatDto.SessionSummary;
import com.smashstep.datn.chat.service.ChatService;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/staff")
public class StaffChatController {
    private final ChatService chatService;

    public StaffChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionSummary>> sessions(@RequestParam(defaultValue = "false") boolean includeClosed) {
        return ApiResponse.ok(chatService.staffSessions(includeClosed));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<SessionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(chatService.staffDetail(id));
    }

    @PostMapping("/sessions/{id}/accept")
    public ApiResponse<SessionDetail> accept(@PathVariable Long id) {
        return ApiResponse.ok("Đã tiếp nhận phiên chat", chatService.accept(id));
    }

    @PostMapping("/sessions/{id}/message")
    public ApiResponse<SessionDetail> send(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.staffSend(id, request.content()));
    }

    @PostMapping("/sessions/{id}/close")
    public ApiResponse<SessionDetail> close(@PathVariable Long id) {
        return ApiResponse.ok("Đã đóng phiên chat", chatService.close(id));
    }
}
