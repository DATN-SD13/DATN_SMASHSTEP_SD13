package com.smashstep.datn.invoice.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.invoice.dto.CapNhatTrangThaiHoaDonDto;
import com.smashstep.datn.invoice.dto.HoaDonChiTietDto;
import com.smashstep.datn.invoice.dto.HoaDonDetailDto;
import com.smashstep.datn.invoice.dto.HoaDonListDto;
import com.smashstep.datn.invoice.dto.LichSuHoaDonDto;
import com.smashstep.datn.invoice.dto.LichSuThanhToanDto;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;
import com.smashstep.datn.invoice.repository.HoaDonChiTietRepository;
import com.smashstep.datn.invoice.repository.HoaDonRepository;
import com.smashstep.datn.invoice.repository.LichSuHoaDonRepository;
import com.smashstep.datn.invoice.repository.LichSuThanhToanRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoaDonService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuThanhToanRepository lichSuThanhToanRepository;
    private final NhanVienRepository nhanVienRepository;

    @Transactional(readOnly = true)
    public PageResponse<HoaDonListDto> timKiem(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            String trangThai,
            String loaiDon,
            int page,
            int size) {
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            throw AppException.badRequest("Từ ngày không được sau đến ngày");
        }
        int pageNumber = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 100);
        Integer status = getTrangThai(trangThai);
        Integer type = getLoaiDon(loaiDon);
        Pageable pageable = PageRequest.of(
                pageNumber - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "ngayTao")
        );
        Specification<HoaDon> specification = taoBoLoc(ma, tuNgay, denNgay, status, type);
        Page<HoaDon> result = hoaDonRepository.findAll(specification, pageable);
        return PageResponse.from(result.map(HoaDonListDto::from));
    }

    @Transactional(readOnly = true)
    public HoaDonDetailDto chiTiet(String ma) {
        HoaDon hoaDon = timHoaDon(ma);
        List<HoaDonChiTietDto> items = hoaDonChiTietRepository
                .findByIdHoaDonIdOrderByIdAsc(hoaDon.getId())
                .stream()
                .map(HoaDonChiTietDto::from)
                .toList();
        List<LichSuThanhToanDto> paymentHistory = lichSuThanhToanRepository
                .findByIdHoaDonIdOrderByThoiGianDesc(hoaDon.getId())
                .stream()
                .map(LichSuThanhToanDto::from)
                .toList();
        List<LichSuHoaDonDto> history = layLichSu(hoaDon.getId());
        return HoaDonDetailDto.from(hoaDon, items, paymentHistory, history);
    }

    @Transactional(readOnly = true)
    public List<LichSuHoaDonDto> lichSu(String ma) {
        HoaDon hoaDon = timHoaDon(ma);
        return layLichSu(hoaDon.getId());
    }

    @Transactional
    public HoaDonDetailDto capNhatTrangThai(String ma, CapNhatTrangThaiHoaDonDto request) {
        if (request.getTrangThai() == null) {
            throw AppException.badRequest("Trạng thái không được để trống");
        }
        TrangThaiHoaDon trangThai = TrangThaiHoaDon.tuMa(request.getTrangThai());
        if (trangThai == null) {
            throw AppException.badRequest("Trạng thái hóa đơn không hợp lệ");
        }
        HoaDon hoaDon = timHoaDon(ma);
        Integer trangThaiHienTai = hoaDon.getTrangThai();

        if (request.getTrangThai() == TrangThaiHoaDon.DA_HUY.getMa()) {
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_THANH.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn thành, không thể hủy.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.DA_HUY.getMa()) {
                throw AppException.badRequest("Đơn hàng đã được hủy, không thể hủy lại.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_TIEN.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn tiền, không thể hủy.");
            }
            if (trangThaiHienTai == null || trangThaiHienTai < TrangThaiHoaDon.CHO_XAC_NHAN.getMa()
                    || trangThaiHienTai > TrangThaiHoaDon.DA_GIAO_HANG.getMa()) {
                throw AppException.badRequest("Đơn hàng hiện tại không thể hủy.");
            }
        } else {
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_THANH.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn thành, không thể cập nhật trạng thái.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.DA_HUY.getMa()) {
                throw AppException.badRequest("Đơn hàng đã được hủy, không thể cập nhật trạng thái.");
            }
            if (trangThaiHienTai != null && trangThaiHienTai == TrangThaiHoaDon.HOAN_TIEN.getMa()) {
                throw AppException.badRequest("Đơn hàng đã hoàn tiền, không thể cập nhật trạng thái.");
            }
            TrangThaiHoaDon trangThaiKeTiep = TrangThaiHoaDon.trangThaiTiepTheo(trangThaiHienTai);
            if (trangThaiKeTiep == null || request.getTrangThai() != trangThaiKeTiep.getMa()) {
                throw AppException.badRequest("Chỉ được chuyển sang trạng thái kế tiếp của đơn hàng.");
            }
        }

        Long idNhanVienThaoTac = request.getIdNhanVien();
        if (idNhanVienThaoTac == null && hoaDon.getIdNhanVien() != null) {
            idNhanVienThaoTac = hoaDon.getIdNhanVien().getId();
        }
        if (idNhanVienThaoTac != null) {
            NhanVien nhanVien = nhanVienRepository.findById(idNhanVienThaoTac)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy nhân viên"));
            hoaDon.setIdNhanVien(nhanVien);
        }
        hoaDon.setTrangThai(request.getTrangThai());
        hoaDon.setNgayCapNhat(LocalDateTime.now());

        hoaDonRepository.save(hoaDon);
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setIdHoaDon(hoaDon);
        lichSu.setNguoiTao(idNhanVienThaoTac);
        lichSu.setTrangThai(request.getTrangThai());
        lichSu.setGhiChu(request.getGhiChu());
        lichSu.setNgayTao(LocalDateTime.now());
        lichSuHoaDonRepository.save(lichSu);
        return chiTiet(ma);
    }

    private List<LichSuHoaDonDto> layLichSu(Long idHoaDon) {
        return lichSuHoaDonRepository
                .findByIdHoaDonIdOrderByNgayTaoDesc(idHoaDon)
                .stream()
                .map(lichSu -> {
                    NhanVien nhanVien = null;
                    if (lichSu.getNguoiTao() != null) {
                        nhanVien = nhanVienRepository.findById(lichSu.getNguoiTao()).orElse(null);
                    }
                    return LichSuHoaDonDto.from(lichSu, nhanVien);
                })
                .toList();
    }

    private HoaDon timHoaDon(String ma) {
        if (ma == null || ma.isBlank()) {
            throw AppException.badRequest("Mã hóa đơn không được để trống");
        }
        return hoaDonRepository.findByMaHoaDon(ma.trim())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hóa đơn: " + ma));
    }

    private Integer getTrangThai(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return null;
        }
        try {
            Integer value = Integer.valueOf(trangThai);
            if (TrangThaiHoaDon.tuMa(value) == null) {
                throw AppException.badRequest("Trạng thái hóa đơn không hợp lệ");
            }
            return value;
        } catch (NumberFormatException e) {
            throw AppException.badRequest("Trạng thái hóa đơn không hợp lệ");
        }
    }


    private Integer getLoaiDon(String loaiDon) {
        if (loaiDon == null || loaiDon.isBlank()) {
            return null;
        }
        LoaiHoaDon loai = LoaiHoaDon.phanTich(loaiDon);
        if (loai == null) {
            throw AppException.badRequest("Loại hóa đơn không hợp lệ");
        }
        return loai.getMa();
    }

    private Specification<HoaDon> taoBoLoc(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            Integer trangThai,
            Integer loaiHoaDon) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ma != null && !ma.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("maHoaDon")),
                        "%" + ma.trim().toLowerCase() + "%"
                ));
            }
            if (tuNgay != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("ngayTao"),
                        tuNgay.atStartOfDay()
                ));
            }
            if (denNgay != null) {
                predicates.add(cb.lessThan(
                        root.get("ngayTao"),
                        denNgay.plusDays(1).atStartOfDay()
                ));
            }
            if (trangThai != null) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai));
            }
            if (loaiHoaDon != null) {
                predicates.add(cb.equal(root.get("loaiHoaDon"), loaiHoaDon));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
