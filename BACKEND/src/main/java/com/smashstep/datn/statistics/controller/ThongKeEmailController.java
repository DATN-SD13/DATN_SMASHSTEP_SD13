package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.statistics.service.StatisticsReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
public class ThongKeEmailController {
    private final StatisticsReportService reports;
    public ThongKeEmailController(StatisticsReportService reports){this.reports=reports;}
    public record EmailRequest(@NotBlank @Email String email,@NotNull LocalDate tuNgay,@NotNull LocalDate denNgay){}
    @PostMapping("/gui-email-tong-ket")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> guiEmailTongKet(@Valid @RequestBody EmailRequest request){
        reports.send(request.email(),request.tuNgay(),request.denNgay());
        return ApiResponse.ok("Gửi báo cáo tổng kết thành công");
    }
}
