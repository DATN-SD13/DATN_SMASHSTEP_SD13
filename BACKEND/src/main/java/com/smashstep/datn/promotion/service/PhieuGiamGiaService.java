package com.smashstep.datn.promotion.service;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.promotion.response.PhieuGiamGiaResponse;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaRepository;
import com.smashstep.datn.promotion.specification.PhieuGiamGiaSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.smashstep.datn.promotion.response.PhieuGiamGiaRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public PageResponse<PhieuGiamGiaResponse> getAll(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            Integer trangThai,
            int page,
            int size
    ) {

        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 5;
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "ngayTao"
                )
        );

        Specification<PhieuGiamGia> specification =
                Specification.allOf(
                        PhieuGiamGiaSpecification.ma(ma),
                        PhieuGiamGiaSpecification.tuNgay(tuNgay),
                        PhieuGiamGiaSpecification.denNgay(denNgay),
                        PhieuGiamGiaSpecification.trangThai(trangThai)
                );

        Page<PhieuGiamGia> result =
                phieuGiamGiaRepository.findAll(
                        specification,
                        pageable
                );

        Page<PhieuGiamGiaResponse> responsePage =
                result.map(this::convertToResponse);

        return PageResponse.from(responsePage);
    }

    private PhieuGiamGiaResponse convertToResponse(
            PhieuGiamGia p
    ) {

        PhieuGiamGiaResponse response =
                new PhieuGiamGiaResponse();

        response.setId(p.getId());

        response.setCode(
                p.getMaPhieuGiamGia()
        );

        response.setName(
                p.getTenPhieuGiamGia()
        );

        response.setForm(
                p.getHinhThucPhieu()
        );

        response.setFormLabel(
                getFormLabel(p.getHinhThucPhieu())
        );

        response.setDiscountType(
                p.getLoaiGiamGia()
        );

        response.setDiscountTypeLabel(
                getDiscountTypeLabel(
                        p.getLoaiGiamGia()
                )
        );

        response.setDiscountValue(
                p.getGiaTriGiam()
        );

        response.setMinOrderValue(
                p.getGiaTriToiThieu()
        );

        response.setMaxDiscount(
                p.getGiamToiDa()
        );

        response.setStartDate(
                p.getNgayBatDau() != null
                        ? p.getNgayBatDau()
                        .toLocalDate()
                        .toString()
                        : null
        );

        response.setEndDate(
                p.getNgayKetThuc() != null
                        ? p.getNgayKetThuc()
                        .toLocalDate()
                        .toString()
                        : null
        );

        response.setQuantity(
                p.getSoLuong()
        );

        response.setUsedQuantity(
                p.getSoLuongDaDung()
        );

        response.setStatus(
                p.getTrangThai()
        );

        response.setStatusLabel(
                getStatusLabel(
                        p.getTrangThai()
                )
        );

        response.setDescription(
                p.getMoTa()
        );

        return response;
    }

    private String getFormLabel(Integer value) {

        if (value == null) {
            return "";
        }

        return switch (value) {
            case 1 -> "Công khai";
            case 2 -> "Cá nhân";
            default -> "Không xác định";
        };
    }

    private String getDiscountTypeLabel(Integer value) {

        if (value == null) {
            return "";
        }

        return switch (value) {
            case 1 -> "Phần trăm";
            case 2 -> "Tiền mặt";
            default -> "Không xác định";
        };
    }

    private String getStatusLabel(Integer value) {

        if (value == null) {
            return "";
        }

        return switch (value) {
            case 1 -> "Hoạt động";
            case 0 -> "Ngừng hoạt động";
            default -> "Không xác định";
        };
    }

    public PhieuGiamGiaResponse getById(Long id) {

        PhieuGiamGia p = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy phiếu giảm giá")
                );

        return convertToResponse(p);
    }
// tao moi
    public PhieuGiamGiaResponse create(
            PhieuGiamGiaRequest request
    ) {

        if (request.getCode() == null ||
                request.getCode().trim().isEmpty()) {

            throw new RuntimeException("Mã phiếu không được để trống");
        }

        if (phieuGiamGiaRepository
                .existsByMaPhieuGiamGia(request.getCode().trim())) {

            throw new RuntimeException("Mã phiếu đã tồn tại");
        }

        validateRequest(request);

        PhieuGiamGia p = new PhieuGiamGia();

        p.setMaPhieuGiamGia(request.getCode().trim());
        p.setTenPhieuGiamGia(request.getName());

        p.setHinhThucPhieu(request.getForm());
        p.setLoaiGiamGia(request.getDiscountType());

        p.setGiaTriGiam(request.getDiscountValue());
        p.setGiaTriToiThieu(request.getMinOrderValue());
        p.setGiamToiDa(request.getMaxDiscount());

        p.setNgayBatDau(
                LocalDateTime.parse(
                        request.getStartDate() + "T00:00:00"
                )
        );

        p.setNgayKetThuc(
                LocalDateTime.parse(
                        request.getEndDate() + "T23:59:59"
                )
        );

        p.setSoLuong(request.getQuantity());
        p.setSoLuongDaDung(0);

        p.setTrangThai(
                request.getStatus() == null
                        ? 1
                        : request.getStatus()
        );

        p.setMoTa(request.getDescription());

        LocalDateTime now = LocalDateTime.now();

        p.setNgayTao(now);
        p.setNgayCapNhat(now);

        PhieuGiamGia saved =
                phieuGiamGiaRepository.save(p);

        return convertToResponse(saved);
    }
// update
    public PhieuGiamGiaResponse update(
            Long id,
            PhieuGiamGiaRequest request
    ) {

        PhieuGiamGia p =
                phieuGiamGiaRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy phiếu giảm giá"
                                )
                        );

        if (request.getCode() == null ||
                request.getCode().trim().isEmpty()) {

            throw new RuntimeException(
                    "Mã phiếu không được để trống"
            );
        }

        if (phieuGiamGiaRepository
                .existsByMaPhieuGiamGiaAndIdNot(
                        request.getCode().trim(),
                        id
                )) {

            throw new RuntimeException(
                    "Mã phiếu đã tồn tại"
            );
        }

        validateRequest(request);

        p.setMaPhieuGiamGia(request.getCode().trim());
        p.setTenPhieuGiamGia(request.getName());

        p.setHinhThucPhieu(request.getForm());
        p.setLoaiGiamGia(request.getDiscountType());

        p.setGiaTriGiam(request.getDiscountValue());
        p.setGiaTriToiThieu(request.getMinOrderValue());
        p.setGiamToiDa(request.getMaxDiscount());

        p.setNgayBatDau(
                LocalDateTime.parse(
                        request.getStartDate() + "T00:00:00"
                )
        );

        p.setNgayKetThuc(
                LocalDateTime.parse(
                        request.getEndDate() + "T23:59:59"
                )
        );

        p.setSoLuong(request.getQuantity());

        p.setTrangThai(
                request.getStatus() == null
                        ? p.getTrangThai()
                        : request.getStatus()
        );

        p.setMoTa(request.getDescription());

        p.setNgayCapNhat(LocalDateTime.now());

        PhieuGiamGia saved =
                phieuGiamGiaRepository.save(p);

        return convertToResponse(saved);
    }
// xoa mem
    public void deactivate(Long id) {

        PhieuGiamGia p =
                phieuGiamGiaRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy phiếu giảm giá"
                                )
                        );

        p.setTrangThai(0);
        p.setNgayCapNhat(LocalDateTime.now());

        phieuGiamGiaRepository.save(p);
    }
    public void activate(Long id) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));

        phieu.setTrangThai(1);
        phieu.setNgayCapNhat(LocalDateTime.now());

        phieuGiamGiaRepository.save(phieu);
    }

    @Transactional
    public void delete(Long id) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy phiếu giảm giá"));

        phieuGiamGiaRepository.delete(phieu);
    }
// validate
    private void validateRequest(
            PhieuGiamGiaRequest request
    ) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Tên phiếu không được để trống"
            );
        }

        if (request.getForm() == null ||
                (request.getForm() != 1 &&
                        request.getForm() != 2)) {

            throw new RuntimeException(
                    "Hình thức phiếu không hợp lệ"
            );
        }

        if (request.getDiscountType() == null ||
                (request.getDiscountType() != 1 &&
                        request.getDiscountType() != 2)) {

            throw new RuntimeException(
                    "Loại giảm giá không hợp lệ"
            );
        }

        if (request.getDiscountValue() == null ||
                request.getDiscountValue()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Giá trị giảm không hợp lệ"
            );
        }

        if (request.getMinOrderValue() != null &&
                request.getMinOrderValue()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Giá trị đơn tối thiểu không hợp lệ"
            );
        }

        if (request.getMaxDiscount() != null &&
                request.getMaxDiscount()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Giảm tối đa không hợp lệ"
            );
        }

        if (request.getQuantity() == null ||
                request.getQuantity() < 0) {

            throw new RuntimeException(
                    "Số lượng không hợp lệ"
            );
        }

        if (request.getStartDate() == null ||
                request.getEndDate() == null) {

            throw new RuntimeException(
                    "Ngày bắt đầu và ngày kết thúc không được để trống"
            );
        }

        if (request.getEndDate()
                .compareTo(request.getStartDate()) < 0) {

            throw new RuntimeException(
                    "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu"
            );
        }

        if (request.getDiscountType() == 1 &&
                request.getDiscountValue()
                        .compareTo(new BigDecimal("100")) > 0) {

            throw new RuntimeException(
                    "Giảm phần trăm không được lớn hơn 100%"
            );
        }
    }
}