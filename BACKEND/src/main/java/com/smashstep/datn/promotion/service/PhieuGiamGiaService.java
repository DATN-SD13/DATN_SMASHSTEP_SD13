package com.smashstep.datn.promotion.service;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.common.config.DatabaseCapabilities;
import com.smashstep.datn.promotion.response.PhieuGiamGiaResponse;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaRepository;
import com.smashstep.datn.promotion.specification.PhieuGiamGiaSpecification;
import org.springframework.transaction.annotation.Transactional;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import com.smashstep.datn.promotion.entity.PhieuGiamGiaKhachHang;
import com.smashstep.datn.promotion.repository.PhieuGiamGiaKhachHangRepository;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhieuGiamGiaService {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PhieuGiamGiaKhachHangRepository assignments;
    private final KhachHangRepository customers;
    private final DatabaseCapabilities database;
    private final EmailService emailService;

    public boolean supportsForm() {
        return database.hasColumn("phieu_giam_gia", "hinh_thuc_phieu");
    }

    private void requireFormSupport() {
        if (!supportsForm()) {
            throw AppException.badRequest("Database hiện tại thiếu hình thức phiếu theo schema chuẩn; không thể lưu hoặc lọc công khai/cá nhân");
        }
    }

    public PageResponse<PhieuGiamGiaResponse> getAll(
            String ma,
            Integer hinhThuc,
            LocalDate tuNgay,
            LocalDate denNgay,
            Integer loaiGiam,
            Integer trangThai,
            int page,
            int size) {

        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 5;
        }
        size = Math.min(size, 100);
        if (hinhThuc != null) requireFormSupport();
        if (hinhThuc != null && hinhThuc != 1 && hinhThuc != 2) {
            throw AppException.badRequest("Hình thức phiếu không hợp lệ");
        }
        if (tuNgay != null && denNgay != null && denNgay.isBefore(tuNgay)) {
            throw AppException.badRequest("Khoảng ngày không hợp lệ");
        }

        Specification<PhieuGiamGia> specification =
                Specification
                        .where(PhieuGiamGiaSpecification.notDeleted())
                        .and(PhieuGiamGiaSpecification.ma(ma))
                        .and(PhieuGiamGiaSpecification.hinhThuc(hinhThuc))
                        .and(PhieuGiamGiaSpecification.tuNgay(tuNgay))
                        .and(PhieuGiamGiaSpecification.denNgay(denNgay))
                        .and(PhieuGiamGiaSpecification.loaiGiam(loaiGiam))
                        .and(PhieuGiamGiaSpecification.trangThai(trangThai));

        if ((long) (page - 1) * (size) > Integer.MAX_VALUE) throw AppException.badRequest("Trang yêu cầu vượt giới hạn phân trang");
        Pageable pageable =
                PageRequest.of(
                        page - 1,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "ngayTao"
                        )
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

        List<Long> customerIds = activeCustomerIds(p);
        response.setForm(
                p.getHinhThucPhieu()
        );

        response.setFormLabel(
                getFormLabel(response.getForm())
        );
        response.setCustomerIds(customerIds);

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
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        : null
        );

        response.setEndDate(
                p.getNgayKetThuc() != null
                        ? p.getNgayKetThuc()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        : null
        );

        response.setQuantity(
                p.getSoLuong()
        );

        response.setUnlimited(
                Boolean.TRUE.equals(p.getVoHan())
        );

        response.setUsedQuantity(
                p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung()
        );

        response.setStatus(
                p.getTrangThai()
        );

        response.setStatusLabel(
                getStatusLabel(
                        p.getTrangThai()
                )
        );
// trang thai pgg theo tgian thuc
        Integer timeStatus = getTimeStatus(p);

        response.setTimeStatus(timeStatus);

        response.setTimeStatusLabel(
                getTimeStatusLabel(timeStatus)
        );

        response.setDescription(
                p.getMoTa()
        );

        return response;
    }

    private String getFormLabel(Integer value) {

        if (value == null) {
            return "Không xác định";
        }

        return switch (value) {
            case 1 -> "Công khai";
            case 2 -> "Cá nhân";
            default -> "Không xác định";
        };
    }

    private String getDiscountTypeLabel(Integer value) {

        if (value == null) {
            return "Không xác định";
        }

        return switch (value) {
            case 1 -> "Phần trăm";
            case 2 -> "Tiền mặt";
            default -> "Không xác định";
        };
    }

    private String getStatusLabel(Integer value) {

        if (value == null) {
            return "Không xác định";
        }

        return switch (value) {
            case 1 -> "Hoạt động";
            case 0 -> "Ngừng hoạt động";
            default -> "Không xác định";
        };
    }

    public PhieuGiamGiaResponse getById(Long id) {

        PhieuGiamGia p = phieuGiamGiaRepository.findById(id)
                .filter(voucher -> !Integer.valueOf(-1).equals(voucher.getTrangThai()))
                .orElseThrow(() ->
                        AppException.notFound("Không tìm thấy phiếu giảm giá")
                );

        return convertToResponse(p);
    }
// tao moi
    @Transactional
    public PhieuGiamGiaResponse create(
            PhieuGiamGiaRequest request

    ) {
        if (request == null) throw AppException.badRequest("Dữ liệu phiếu không hợp lệ");
        if (request.getCode() == null ||
                request.getCode().trim().isEmpty()) {
            throw AppException.badRequest("Mã phiếu không được để trống");
        }

        if (phieuGiamGiaRepository
                .existsByMaPhieuGiamGia(request.getCode().trim())) {
            throw AppException.conflict("Mã phiếu đã tồn tại");
        }

        validateRequest(request);

        PhieuGiamGia p = new PhieuGiamGia();
        requireFormSupport();

        p.setMaPhieuGiamGia(request.getCode().trim());
        p.setTenPhieuGiamGia(request.getName().trim());

        p.setHinhThucPhieu(request.getForm());
        p.setLoaiGiamGia(request.getDiscountType());

        p.setGiaTriGiam(request.getDiscountValue());
        p.setGiaTriToiThieu(request.getMinOrderValue());
        p.setGiamToiDa(request.getMaxDiscount());

        p.setNgayBatDau(
                LocalDateTime.parse(
                        request.getStartDate()
                )
        );

        p.setNgayKetThuc(
                LocalDateTime.parse(
                        request.getEndDate()
                )
        );

        if (Boolean.TRUE.equals(request.getUnlimited())) {
            p.setVoHan(true);
            p.setSoLuong(null);
        } else {
            p.setVoHan(false);
            p.setSoLuong(request.getQuantity());
        }

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
        syncAssignments(saved, request);

        if (Integer.valueOf(2).equals(saved.getHinhThucPhieu())) {
            sendEmailsAfterCommit(saved, request.getCustomerIds());
        }
        return convertToResponse(saved);

    }
// update
    @Transactional
    public PhieuGiamGiaResponse update(
            Long id,
            PhieuGiamGiaRequest request
    ) {

        PhieuGiamGia p =
                phieuGiamGiaRepository.findByIdForUpdate(id)
                        .filter(voucher -> !Integer.valueOf(-1).equals(voucher.getTrangThai()))
                        .orElseThrow(() ->
                                AppException.notFound("Không tìm thấy phiếu giảm giá")
                        );


        validateRequest(request);
        if (!Boolean.TRUE.equals(request.getUnlimited()) && p.getSoLuongDaDung() != null
                && request.getQuantity() < p.getSoLuongDaDung()) {
            throw AppException.badRequest("Số lượng không được nhỏ hơn số phiếu đã sử dụng");
        }

        requireFormSupport();

        Integer oldTimeStatus = getTimeStatus(p);
        Integer oldForm = p.getHinhThucPhieu();
        List<Long> oldCustomerIds = activeCustomerIds(p);

        String oldTen = p.getTenPhieuGiamGia();
        Integer oldLoaiGiam = p.getLoaiGiamGia();
        BigDecimal oldGiaTriGiam = p.getGiaTriGiam();
        BigDecimal oldGiaTriToiThieu = p.getGiaTriToiThieu();
        BigDecimal oldGiamToiDa = p.getGiamToiDa();
        LocalDateTime oldNgayBatDau = p.getNgayBatDau();
        LocalDateTime oldNgayKetThuc = p.getNgayKetThuc();

        LocalDateTime newStart = LocalDateTime.parse(request.getStartDate());
        LocalDateTime newEnd = LocalDateTime.parse(request.getEndDate());

        boolean isInfoChanged = !java.util.Objects.equals(oldTen, request.getName().trim())
                || !java.util.Objects.equals(oldLoaiGiam, request.getDiscountType())
                || isBigDecimalDifferent(oldGiaTriGiam, request.getDiscountValue())
                || isBigDecimalDifferent(oldGiaTriToiThieu, request.getMinOrderValue())
                || isBigDecimalDifferent(oldGiamToiDa, request.getMaxDiscount())
                || !java.util.Objects.equals(oldNgayBatDau, newStart)
                || !java.util.Objects.equals(oldNgayKetThuc, newEnd);

        List<Long> newCustomerIds = (request.getForm() != null && request.getForm() == 2 && request.getCustomerIds() != null)
                ? request.getCustomerIds().stream().filter(java.util.Objects::nonNull).distinct().toList()
                : List.of();

        List<Long> addedIds = newCustomerIds.stream()
                .filter(cId -> !oldCustomerIds.contains(cId))
                .toList();
        List<Long> removedIds = oldCustomerIds.stream()
                .filter(cId -> !newCustomerIds.contains(cId))
                .toList();
        List<Long> retainedIds = newCustomerIds.stream()
                .filter(oldCustomerIds::contains)
                .toList();

        p.setTenPhieuGiamGia(request.getName().trim());

        p.setHinhThucPhieu(request.getForm());
        p.setLoaiGiamGia(request.getDiscountType());

        p.setGiaTriGiam(request.getDiscountValue());
        p.setGiaTriToiThieu(request.getMinOrderValue());
        p.setGiamToiDa(request.getMaxDiscount());

        p.setNgayBatDau(newStart);
        p.setNgayKetThuc(newEnd);

        if (Boolean.TRUE.equals(request.getUnlimited())) {
            p.setVoHan(true);
            p.setSoLuong(null);
        } else {
            p.setVoHan(false);
            p.setSoLuong(request.getQuantity());
        }

        p.setTrangThai(
                request.getStatus() == null
                        ? p.getTrangThai()
                        : request.getStatus()
        );

        p.setMoTa(request.getDescription());

        p.setNgayCapNhat(LocalDateTime.now());

        PhieuGiamGia saved =
                phieuGiamGiaRepository.save(p);
        syncAssignments(saved, request);

        if (oldTimeStatus != null && oldTimeStatus != 3 && (Integer.valueOf(2).equals(oldForm) || Integer.valueOf(2).equals(saved.getHinhThucPhieu()))) {
            sendEmailsOnUpdateAfterCommit(saved, addedIds, removedIds, retainedIds, isInfoChanged);
        }

        return convertToResponse(saved);
    }
// xoa mem
    @Transactional
    public void deactivate(Long id) {

        PhieuGiamGia p =
                phieuGiamGiaRepository.findByIdForUpdate(id)
                        .filter(voucher -> !Integer.valueOf(-1).equals(voucher.getTrangThai()))
                        .orElseThrow(() ->
                                AppException.notFound("Không tìm thấy phiếu giảm giá")
                        );

        p.setTrangThai(0);
        p.setNgayCapNhat(LocalDateTime.now());

        phieuGiamGiaRepository.save(p);
    }
    @Transactional
    public void activate(Long id) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findByIdForUpdate(id)
                .filter(voucher -> !Integer.valueOf(-1).equals(voucher.getTrangThai()))
                .orElseThrow(() -> AppException.notFound("Không tìm thấy phiếu giảm giá"));

        phieu.setTrangThai(1);
        phieu.setNgayCapNhat(LocalDateTime.now());

        phieuGiamGiaRepository.save(phieu);
    }

    @Transactional
    public void delete(Long id) {
        PhieuGiamGia phieu = phieuGiamGiaRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        AppException.notFound("Không tìm thấy phiếu giảm giá"));

        phieu.setTrangThai(-1);
        phieu.setNgayCapNhat(LocalDateTime.now());
        phieuGiamGiaRepository.save(phieu);
    }
// validate
    private void validateRequest(
            PhieuGiamGiaRequest request
    ) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw AppException.badRequest(
                    "Tên phiếu không được để trống"
            );
        }
        for (BigDecimal value : new BigDecimal[]{request.getDiscountValue(), request.getMinOrderValue(), request.getMaxDiscount()}) {
            if (value != null && (value.scale() > 2 || value.precision() - value.scale() > 16)) {
                throw AppException.badRequest("Giá trị tiền chỉ được có tối đa 16 chữ số và 2 chữ số thập phân");
            }
        }
        if (request.getName().trim().length() > 255
                || (request.getCode() != null && request.getCode().trim().length() > 50)
                || (request.getDescription() != null && request.getDescription().length() > 1000)) {
            throw AppException.badRequest("Mã, tên hoặc mô tả vượt quá độ dài cho phép");
        }
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw AppException.badRequest("Trạng thái chỉ được phép là 0 hoặc 1");
        }

        if (request.getForm() == null ||
                (request.getForm() != 1 &&
                        request.getForm() != 2)) {

            throw AppException.badRequest(
                    "Hình thức phiếu không hợp lệ"
            );
        }
        if (request.getForm() == 2) {
            if (request.getCustomerIds() == null || request.getCustomerIds().isEmpty()) {
                throw AppException.badRequest("Phiếu cá nhân cần ít nhất một khách hàng");
            }
            for (Long customerId : request.getCustomerIds().stream().distinct().toList()) {
                if (customerId == null || customerId <= 0) {
                    throw AppException.badRequest("Khách hàng không hợp lệ");
                }
                KhachHang customer = customers.findById(customerId)
                        .orElseThrow(() -> AppException.notFound("Không tìm thấy khách hàng ID: " + customerId));
                if (!Integer.valueOf(1).equals(customer.getTrangThai())) {
                    throw AppException.badRequest("Khách hàng nhận phiếu phải đang hoạt động");
                }
            }
        }

        if (request.getForm() == 2) {
            long customerCount = request.getCustomerIds()
                    .stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();

            if (request.getQuantity() == null ||
                    request.getQuantity().longValue() != customerCount) {
                throw AppException.badRequest(
                        "Số lượng phiếu phải bằng số khách hàng được chọn"
                );
            }
        }

        if (request.getDiscountType() == null ||
                (request.getDiscountType() != 1 &&
                        request.getDiscountType() != 2)) {

            throw AppException.badRequest(
                    "Loại giảm giá không hợp lệ"
            );
        }

        if (request.getDiscountValue() == null ||
                request.getDiscountValue()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw AppException.badRequest(
                    "Giá trị giảm không hợp lệ"
            );
        }

        if (request.getMinOrderValue() == null ||
                request.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw AppException.badRequest(
                    "Giá trị đơn tối thiểu phải lớn hơn hoặc bằng 0"
            );
        }

        if (request.getDiscountType() == 1 &&
                request.getMaxDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.badRequest(
                    "Giảm theo phần trăm phải có mức giảm tối đa lớn hơn 0"
            );
        }

        if (!Boolean.TRUE.equals(request.getUnlimited())) {
            if (request.getQuantity() == null ||
                    request.getQuantity() <= 0) {
                throw AppException.badRequest(
                        "Số lượng phải lớn hơn 0"
                );
            }
        }

        if (request.getStartDate() == null ||
                request.getEndDate() == null) {

            throw AppException.badRequest(
                    "Ngày bắt đầu và ngày kết thúc không được để trống"
            );
        }

        LocalDateTime start;
        LocalDateTime end;

        try {
            start = LocalDateTime.parse(request.getStartDate());
            end = LocalDateTime.parse(request.getEndDate());
        } catch (DateTimeParseException ex) {
            throw AppException.badRequest(
                    "Ngày giờ bắt đầu và kết thúc không đúng định dạng"
            );
        }

        if (!end.isAfter(start)) {
            throw AppException.badRequest(
                    "Ngày giờ kết thúc phải sau ngày giờ bắt đầu"
            );
        }

        if (request.getDiscountType() == 1 &&
                request.getDiscountValue()
                        .compareTo(new BigDecimal("100")) > 0) {

            throw AppException.badRequest(
                    "Giảm phần trăm không được lớn hơn 100%"
            );
        }
    }

    private List<Long> activeCustomerIds(PhieuGiamGia voucher) {
        if (voucher.getId() == null) return List.of();
        return assignments.findByIdPhieuGiamGia_Id(voucher.getId()).stream()
                .filter(link -> Integer.valueOf(1).equals(link.getTrangThai()) && link.getIdKhachHang() != null)
                .map(link -> link.getIdKhachHang().getId()).distinct().toList();
    }

    private void syncAssignments(PhieuGiamGia voucher, PhieuGiamGiaRequest request) {
        List<Long> selected = request.getForm() == 2 ? request.getCustomerIds().stream().distinct().toList() : List.of();
        List<PhieuGiamGiaKhachHang> links = assignments.findByIdPhieuGiamGia_Id(voucher.getId());
        Map<Long, PhieuGiamGiaKhachHang> byCustomer = new LinkedHashMap<>();
        for (PhieuGiamGiaKhachHang link : links) {
            Long customerId = link.getIdKhachHang() == null ? null : link.getIdKhachHang().getId();
            boolean first = customerId != null && byCustomer.putIfAbsent(customerId, link) == null;
            link.setTrangThai(first && selected.contains(customerId) ? 1 : 0);
        }
        assignments.saveAll(links);
        for (Long customerId : selected) {
            if (byCustomer.containsKey(customerId)) continue;
            var link = new PhieuGiamGiaKhachHang();
            link.setIdPhieuGiamGia(voucher);
            link.setIdKhachHang(customers.findById(customerId).orElseThrow(
                    () -> AppException.notFound("Không tìm thấy khách hàng ID: " + customerId)));
            link.setTrangThai(1);
            assignments.save(link);
        }
    }
// trang thai cua pgg theo tgian thuc
    private Integer getTimeStatus(PhieuGiamGia p) {

        LocalDateTime now = LocalDateTime.now();

        if (p.getNgayBatDau() == null || p.getNgayKetThuc() == null) {
            return null;
        }

        if (now.isBefore(p.getNgayBatDau())) {
            return 1; // Sắp diễn ra
        }

        if (now.isBefore(p.getNgayKetThuc())) {
            return 2; // Đang diễn ra
        }

        return 3; // Đã kết thúc
    }

    private String getTimeStatusLabel(Integer value) {

        if (value == null) {
            return "Không xác định";
        }

        return switch (value) {
            case 1 -> "Sắp diễn ra";
            case 2 -> "Đang diễn ra";
            case 3 -> "Đã kết thúc";
            default -> "Không xác định";
        };
    }

    private void sendEmailsAfterCommit(
            PhieuGiamGia voucher,
            List<Long> customerIds
    ) {
        if (!Integer.valueOf(2).equals(voucher.getHinhThucPhieu())
                || customerIds == null
                || customerIds.isEmpty()) {
            return;
        }

        List<Long> ids = customerIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        executeAfterCommitOrNow(() -> {
            String discount = formatDiscountString(voucher);
            for (Long customerId : ids) {
                try {
                    KhachHang customer = customers.findById(customerId)
                            .orElse(null);

                    if (customer == null
                            || customer.getEmail() == null
                            || customer.getEmail().isBlank()) {
                        continue;
                    }

                    System.out.println(
                            "Đang gửi email phiếu giảm giá đến: " + customer.getEmail()
                    );

                    emailService.sendVoucherEmail(
                            customer.getEmail(),
                            customer.getTenKhachHang(),
                            voucher.getTenPhieuGiamGia(),
                            voucher.getMaPhieuGiamGia(),
                            discount,
                            String.valueOf(voucher.getGiamToiDa()),
                            String.valueOf(voucher.getGiaTriToiThieu()),
                            String.valueOf(voucher.getNgayBatDau()),
                            String.valueOf(voucher.getNgayKetThuc()),
                            "https://example.com"
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void sendEmailsOnUpdateAfterCommit(
            PhieuGiamGia voucher,
            List<Long> addedIds,
            List<Long> removedIds,
            List<Long> retainedIds,
            boolean isInfoChanged
    ) {
        if (addedIds.isEmpty() && removedIds.isEmpty() && (!isInfoChanged || retainedIds.isEmpty())) {
            return;
        }

        executeAfterCommitOrNow(() -> {
            String discount = formatDiscountString(voucher);

            // 1. Khách hàng mới được thêm vào (TH2, TH3) -> Gửi mail mới
            for (Long customerId : addedIds) {
                try {
                    KhachHang customer = customers.findById(customerId).orElse(null);
                    if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                        continue;
                    }
                    System.out.println("Đang gửi email cấp mới voucher đến: " + customer.getEmail());
                    emailService.sendVoucherEmail(
                            customer.getEmail(),
                            customer.getTenKhachHang(),
                            voucher.getTenPhieuGiamGia(),
                            voucher.getMaPhieuGiamGia(),
                            discount,
                            String.valueOf(voucher.getGiamToiDa()),
                            String.valueOf(voucher.getGiaTriToiThieu()),
                            String.valueOf(voucher.getNgayBatDau()),
                            String.valueOf(voucher.getNgayKetThuc()),
                            "https://example.com"
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // 2. Khách hàng bị loại bỏ khỏi danh sách (TH2, TH3) -> Gửi mail hủy
            for (Long customerId : removedIds) {
                try {
                    KhachHang customer = customers.findById(customerId).orElse(null);
                    if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                        continue;
                    }
                    System.out.println("Đang gửi email hủy voucher đến: " + customer.getEmail());
                    emailService.sendVoucherCancelledEmail(
                            customer.getEmail(),
                            customer.getTenKhachHang(),
                            voucher.getTenPhieuGiamGia(),
                            voucher.getMaPhieuGiamGia()
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // 3. Khách hàng giữ nguyên -> Chỉ gửi nếu thông tin PGG thay đổi (TH1, TH3), nếu không đổi thì bỏ qua (5 -> K)
            if (isInfoChanged) {
                for (Long customerId : retainedIds) {
                    try {
                        KhachHang customer = customers.findById(customerId).orElse(null);
                        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                            continue;
                        }
                        System.out.println("Đang gửi email cập nhật voucher đến: " + customer.getEmail());
                        emailService.sendVoucherUpdatedEmail(
                                customer.getEmail(),
                                customer.getTenKhachHang(),
                                voucher.getTenPhieuGiamGia(),
                                voucher.getMaPhieuGiamGia(),
                                discount,
                                String.valueOf(voucher.getGiamToiDa()),
                                String.valueOf(voucher.getGiaTriToiThieu()),
                                String.valueOf(voucher.getNgayBatDau()),
                                String.valueOf(voucher.getNgayKetThuc()),
                                "https://example.com"
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void executeAfterCommitOrNow(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            java.util.concurrent.CompletableFuture.runAsync(task);
                        }
                    }
            );
        } else {
            java.util.concurrent.CompletableFuture.runAsync(task);
        }
    }

    private String formatDiscountString(PhieuGiamGia voucher) {
        if (voucher.getGiaTriGiam() == null) {
            return "0";
        }
        return voucher.getLoaiGiamGia() == 1
                ? voucher.getGiaTriGiam().stripTrailingZeros().toPlainString() + "%"
                : voucher.getGiaTriGiam().stripTrailingZeros().toPlainString() + " VNĐ";
    }

    private boolean isBigDecimalDifferent(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return a.compareTo(b) != 0;
    }

}
