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
    private final com.smashstep.datn.schedule.repository.LichLamViecRepository lich;
    private final com.smashstep.datn.product.service.KhoaGhiSanPham khoaGhi;

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
        khoaGhi.khoa("schedule");kiemTra(req);
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
        khoaGhi.khoa("schedule");kiemTra(req);
        CaLamViec ca = findById(id);
        if((!ca.getGioBatDau().equals(LocalTime.parse(req.getGioBatDau()))||!ca.getGioKetThuc().equals(LocalTime.parse(req.getGioKetThuc())))&&lich.existsByIdCaLamViec_Id(id))
            throw AppException.conflict("Ca đã được phân lịch, không đổi giờ để tránh làm sai lịch và lịch sử. Hãy tạo ca mới.");

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
        khoaGhi.khoa("schedule");
        CaLamViec ca = findById(id);
        ca.setTrangThai(ca.getTrangThai() == null || ca.getTrangThai() != 1 ? 1 : 0);
        ca.setNgayCapNhat(LocalDateTime.now());
        return toResponse(caLamViecRepository.save(ca));
    }

    private void kiemTra(CaLamViecRequest req){
        if(req==null||req.getTenCa()==null||req.getTenCa().isBlank()||req.getTenCa().length()>255)
            throw AppException.badRequest("Tên ca bắt buộc, tối đa 255 ký tự");
        if(req.getMoTa()!=null&&req.getMoTa().length()>1000)throw AppException.badRequest("Mô tả tối đa 1000 ký tự");
        try{
            var start=LocalTime.parse(req.getGioBatDau());var end=LocalTime.parse(req.getGioKetThuc());
            if(start.equals(end))throw AppException.badRequest("Giờ bắt đầu và kết thúc phải khác nhau; hỗ trợ ca qua đêm");
        }catch(java.time.DateTimeException|NullPointerException e){throw AppException.badRequest("Giờ ca phải đúng HH:mm");}
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
