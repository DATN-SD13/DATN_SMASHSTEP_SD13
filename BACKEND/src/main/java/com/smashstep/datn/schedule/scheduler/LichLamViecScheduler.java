package com.smashstep.datn.schedule.scheduler;

import com.smashstep.datn.schedule.service.LichLamViecService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LichLamViecScheduler {

    private final LichLamViecService lichLamViecService;

    /**
     * Chạy mỗi phút: tự động chuyển lịch quá giờ kết thúc ca
     * (trangThai = 0 hoặc 1) sang "Đã kết thúc" (trangThai = 3).
     */
    @Scheduled(fixedDelay = 60_000)
    public void tuDongKetThucCa() {
        int count = lichLamViecService.tuDongKetThuc();
        if (count > 0) {
            log.info("[Scheduler] Tự động kết thúc {} lịch làm việc quá giờ ca.", count);
        }
    }
}
