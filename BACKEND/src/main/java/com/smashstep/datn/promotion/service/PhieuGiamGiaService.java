package com.smashstep.datn.promotion.service;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.promotion.response.PhieuGiamGiaResponse;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaRepository;
import com.smashstep.datn.promotion.specification.PhieuGiamGiaSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
}