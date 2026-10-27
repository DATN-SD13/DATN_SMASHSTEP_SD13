package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.statistics.service.StatisticsReportSchedule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thong-ke-tong-hop/lich-gui-email")
@PreAuthorize("hasRole('ADMIN')")
public class ThongKeLichEmailController {
    private final StatisticsReportSchedule schedule;
    public ThongKeLichEmailController(StatisticsReportSchedule schedule){this.schedule=schedule;}
    public record ScheduleRequest(String email,boolean monthlyEnabled,boolean yearlyEnabled){}
    @GetMapping public ApiResponse<StatisticsReportSchedule.View> get(){return ApiResponse.ok(schedule.view());}
    @PutMapping public ApiResponse<StatisticsReportSchedule.View> save(@RequestBody ScheduleRequest request){
        return ApiResponse.ok("Đã lưu lịch gửi tự động",schedule.save(request.email(),request.monthlyEnabled(),request.yearlyEnabled()));
    }
}
