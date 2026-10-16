package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.schedule.dto.CapNhatTrangThaiRequest;
import com.smashstep.datn.schedule.dto.LichLamViecRequest;
import com.smashstep.datn.schedule.dto.LichLamViecResponse;
import com.smashstep.datn.schedule.entity.CaLamViec;
import com.smashstep.datn.schedule.entity.LichLamViec;
import com.smashstep.datn.schedule.repository.CaLamViecRepository;
import com.smashstep.datn.schedule.repository.LichLamViecRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;
    private final CaLamViecRepository   caLamViecRepository;
    private final NhanVienRepository    nhanVienRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Danh sách ──────────────────────────────────────────
    public PageResponse<LichLamViecResponse> getAll(
            LocalDate tuNgay, LocalDate denNgay,
            Long caId, String tuKhoa,
            int page, int size) {

        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) throw AppException.badRequest("Khoảng ngày không hợp lệ");
        if (page < 1) page = 1;
        size = Math.min(Math.max(size, 1), 100);

        var pageable = PageRequest.of(page - 1, size);
        var result   = lichLamViecRepository.search(tuNgay, denNgay, caId, tuKhoa, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    public List<LichLamViecResponse> getByWeek(LocalDate tuNgay) {
        LocalDate denNgay = tuNgay.plusDays(6);
        return lichLamViecRepository.findByNgayLamBetween(tuNgay, denNgay)
                .stream().map(this::toResponse).toList();
    }

    // ── Tạo mới ────────────────────────────────────────────
    @Transactional
    public List<LichLamViecResponse> create(LichLamViecRequest request) {
        CaLamViec ca = caLamViecRepository.findById(request.getCaLamViecId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca làm việc"));

        if (!Integer.valueOf(1).equals(ca.getTrangThai())) {
            throw AppException.badRequest("Ca làm việc đã ngừng hoạt động");
        }

        List<LichLamViecResponse> results = new ArrayList<>();

        for (String maNv : request.getMaNhanViens()) {
            NhanVien nv = nhanVienRepository.findByMaNhanVien(maNv)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy nhân viên: " + maNv));

            if (!Integer.valueOf(1).equals(nv.getTrangThai())) {
                throw AppException.badRequest("Nhân viên " + maNv + " đang bị khóa");
            }

            // Kiểm tra trùng ca ngày
            if (lichLamViecRepository.existsByIdNhanVien_MaNhanVienAndIdCaLamViec_IdAndNgayLam(
                    maNv, ca.getId(), request.getNgayLam())) {
                throw AppException.conflict("Nhân viên " + maNv + " đã có lịch trong ca này ngày " + request.getNgayLam());
            }

            LichLamViec llv = new LichLamViec();
            llv.setIdNhanVien(nv);
            llv.setIdCaLamViec(ca);
            llv.setNgayLam(request.getNgayLam());
            llv.setTrangThai(0);
            llv.setGhiChu(request.getGhiChu());
            llv.setNgayTao(LocalDateTime.now());
            llv.setNgayCapNhat(LocalDateTime.now());

            results.add(toResponse(lichLamViecRepository.save(llv)));
        }

        return results;
    }

    // ── Cập nhật trạng thái ────────────────────────────────
    @Transactional
    public LichLamViecResponse updateTrangThai(Long id, CapNhatTrangThaiRequest request) {
        LichLamViec llv = lichLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch làm việc"));

        if (request.getTrangThai() < 0 || request.getTrangThai() > 3) {
            throw AppException.badRequest("Trạng thái không hợp lệ (0-3)");
        }

        llv.setTrangThai(request.getTrangThai());
        if (request.getGhiChu() != null) llv.setGhiChu(request.getGhiChu());
        llv.setNgayCapNhat(LocalDateTime.now());

        return toResponse(lichLamViecRepository.save(llv));
    }

    // ── Xóa ───────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        LichLamViec llv = lichLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch làm việc"));
        lichLamViecRepository.delete(llv);
    }

    // ── Mapping ────────────────────────────────────────────
    private LichLamViecResponse toResponse(LichLamViec llv) {
        NhanVien nv = llv.getIdNhanVien();
        CaLamViec ca = llv.getIdCaLamViec();
        String thoiGian = fmt(ca.getGioBatDau()) + " - " + fmt(ca.getGioKetThuc());

        return LichLamViecResponse.builder()
                .id(llv.getId())
                .maNhanVien(nv.getMaNhanVien())
                .tenNhanVien(nv.getTenNhanVien())
                .hinhAnh(nv.getHinhAnh())
                .caLamViecId(ca.getId())
                .maCa(ca.getMaCa())
                .tenCa(ca.getTenCa())
                .thoiGian(thoiGian)
                .ngayLam(llv.getNgayLam() != null ? llv.getNgayLam().toString() : null)
                .trangThai(llv.getTrangThai())
                .trangThaiLabel(trangThaiLabel(llv.getTrangThai()))
                .ghiChu(llv.getGhiChu())
                .build();
    }

    private String fmt(java.time.LocalTime t) {
        if (t == null) return "";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private String trangThaiLabel(Integer tt) {
        if (tt == null) return "";
        return switch (tt) {
            case 0 -> "Chờ xác nhận";
            case 1 -> "Đã xác nhận";
            case 2 -> "Vắng mặt";
            case 3 -> "Hoàn thành";
            default -> "Không xác định";
        };
    }
}
