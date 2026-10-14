package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.dto.CaLamViecResponse;
import com.smashstep.datn.schedule.entity.CaLamViec;
import com.smashstep.datn.schedule.repository.CaLamViecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaLamViecService {

    private final CaLamViecRepository caLamViecRepository;

    public List<CaLamViecResponse> getAll() {
        return caLamViecRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CaLamViecResponse> getActive() {
        return caLamViecRepository.findByTrangThaiOrderByGioBatDauAsc(1).stream()
                .map(this::toResponse)
                .toList();
    }

    public CaLamViecResponse getById(Long id) {
        return toResponse(findById(id));
    }

    private CaLamViec findById(Long id) {
        return caLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca làm việc"));
    }

    private CaLamViecResponse toResponse(CaLamViec ca) {
        String thoiGian = format(ca.getGioBatDau()) + " - " + format(ca.getGioKetThuc());
        return CaLamViecResponse.builder()
                .id(ca.getId())
                .maCa(ca.getMaCa())
                .tenCa(ca.getTenCa())
                .gioBatDau(format(ca.getGioBatDau()))
                .gioKetThuc(format(ca.getGioKetThuc()))
                .thoiGian(thoiGian)
                .moTa(ca.getMoTa())
                .trangThai(ca.getTrangThai())
                .trangThaiLabel(ca.getTrangThai() == null ? "" : ca.getTrangThai() == 1 ? "Hoạt động" : "Ngừng")
                .build();
    }

    private String format(java.time.LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
