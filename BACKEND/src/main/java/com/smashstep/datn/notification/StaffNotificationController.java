package com.smashstep.datn.notification;
import com.smashstep.datn.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/staff-notifications")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class StaffNotificationController {
    private final StaffNotificationService service;
    public StaffNotificationController(StaffNotificationService service){this.service=service;}
    @GetMapping public ApiResponse<StaffNotificationService.Feed> list(){return ApiResponse.ok(service.list());}
    public record ReadRequest(List<String> ids){}
    @PostMapping("/read") public ApiResponse<StaffNotificationService.Feed> read(@RequestBody ReadRequest request){return ApiResponse.ok(service.markRead(request.ids()));}
}
