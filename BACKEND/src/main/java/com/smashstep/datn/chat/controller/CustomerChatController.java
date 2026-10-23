package com.smashstep.datn.chat.controller;

import com.smashstep.datn.chat.dto.ChatDto.SendMessageRequest;
import com.smashstep.datn.chat.dto.ChatDto.SessionDetail;
import com.smashstep.datn.chat.service.ChatService;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/customer")
public class CustomerChatController {
    private final ChatService chatService;

    public CustomerChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/session")
    public ApiResponse<SessionDetail> current() {
        return ApiResponse.ok(chatService.currentCustomerSession());
    }

    @PostMapping("/session/new")
    public ApiResponse<SessionDetail> startNew() {
        return ApiResponse.ok("Đã bắt đầu phiên chat mới", chatService.startNewCustomerSession());
    }

    @PostMapping("/message")
    public ApiResponse<SessionDetail> send(@Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.customerSend(request.content()));
    }

    @PostMapping("/request-staff")
    public ApiResponse<SessionDetail> requestStaff() {
        return ApiResponse.ok("Đã gửi yêu cầu tới nhân viên", chatService.requestStaff());
    }
}
