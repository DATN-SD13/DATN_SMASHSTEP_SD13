package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.dto.CaLamViecRequest;
import com.smashstep.datn.schedule.dto.CaLamViecResponse;
import com.smashstep.datn.schedule.entity.CaLamViec;
import com.smashstep.datn.schedule.repository.CaLamViecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaLamViecService {

    private final CaLamViecRepository caLamViecRepository;

    public List<CaLamViecResponse> getAll() {
        return caLamViecRepository.findAllByOrderByGioBatDauAsc().stream()
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

    @Transactional
    public CaLamViecResponse create(CaLamViecRequest req) {
        if (caLamViecRepository.existsByTenCaIgnoreCase(req.getTenCa().trim())) {
            throw AppException.conflict("Tên ca \"" + req.getTenCa() + "\" đã tồn tại");
        }

        CaLamViec ca = new CaLamViec();
        ca.setMaCa(sinhMaCa());
        ca.setTenCa(req.getTenCa().trim());
        ca.setGioBatDau(LocalTime.parse(req.getGioBatDau()));
        ca.setGioKetThuc(LocalTime.parse(req.getGioKetThuc()));
        ca.setMoTa(req.getMoTa());
        ca.setTrangThai(1);
        ca.setNgayTao(LocalDateTime.now());
        ca.setNgayCapNhat(LocalDateTime.now());

        return toResponse(caLamViecRepository.save(ca));
    }

    @Transactional
    public CaLamViecResponse update(Long id, CaLamViecRequest req) {
        CaLamViec ca = findById(id);

        if (caLamViecRepository.existsByTenCaIgnoreCaseAndIdNot(req.getTenCa().trim(), id)) {
            throw AppException.conflict("Tên ca \"" + req.getTenCa() + "\" đã tồn tại");
        }

        ca.setTenCa(req.getTenCa().trim());
        ca.setGioBatDau(LocalTime.parse(req.getGioBatDau()));
        ca.setGioKetThuc(LocalTime.parse(req.getGioKetThuc()));
        ca.setMoTa(req.getMoTa());
        ca.setNgayCapNhat(LocalDateTime.now());

        return toResponse(caLamViecRepository.save(ca));
    }

    @Transactional
    public CaLamViecResponse toggleTrangThai(Long id) {
        CaLamViec ca = findById(id);
        ca.setTrangThai(ca.getTrangThai() == null || ca.getTrangThai() != 1 ? 1 : 0);
        ca.setNgayCapNhat(LocalDateTime.now());
        return toResponse(caLamViecRepository.save(ca));
    }

    private CaLamViec findById(Long id) {
        return caLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca làm việc"));
    }

    private String sinhMaCa() {
        return "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
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

    private String format(LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
