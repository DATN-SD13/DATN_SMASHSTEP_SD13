package com.smashstep.datn.customer.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.customer.dto.DiaChiKhachHangRequest;
import com.smashstep.datn.customer.dto.DiaChiKhachHangResponse;
import com.smashstep.datn.customer.dto.KhachHangRequest;
import com.smashstep.datn.customer.dto.KhachHangResponse;
import com.smashstep.datn.customer.dto.TrangThaiKhachHangRequest;
import com.smashstep.datn.customer.entity.DiaChiKhachHang;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.customer.repository.DiaChiKhachHangRepository;
import com.smashstep.datn.customer.repository.KhachHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KhachHangService {

    private final KhachHangRepository khachHangRepository;

    private final DiaChiKhachHangRepository diaChiRepository;

    public PageResponse<KhachHangResponse> getAll(
            String tuKhoa,
            Integer trangThai,
            int page,
            int size
    ) {
        validateStatusFilter(trangThai);

        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.min(Math.max(size, 1), 100);

        if ((long) (safePage) * (safeSize) > Integer.MAX_VALUE) throw AppException.badRequest("Trang yêu cầu vượt giới hạn phân trang");
        Page<KhachHangResponse> result =
                khachHangRepository.search(
                        normalizeKeyword(tuKhoa),
                        trangThai,
                        PageRequest.of(
                                safePage,
                                safeSize,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "id"
                                )
                        )
                ).map(kh -> toResponse(kh, false));

        return PageResponse.from(result);
    }

    public KhachHangResponse getByCode(String code) {
        return toResponse(
                findCustomer(code),
                true
        );
    }

    @Transactional
    public KhachHangResponse create(
            KhachHangRequest request
    ) {
        validateCustomerRequest(request, null);

        if (request.getDefaultAddress() == null) {
            throw AppException.badRequest(
                    "Vui lòng nhập địa chỉ giao hàng mặc định"
            );
        }

        KhachHang customer = new KhachHang();

        customer.setMaKhachHang(null);
        customer.setTenTaiKhoan(null);

        customer.setTenKhachHang(
                clean(request.getName())
        );

        customer.setEmail(
                cleanLower(request.getEmail())
        );

        customer.setSoDienThoai(
                cleanNullable(request.getPhone())
        );

        customer.setNgaySinh(
                request.getDob()
        );

        customer.setGioiTinh(
                request.getGender()
        );

        customer.setMatKhau(null);

        customer.setHinhAnh(
                normalizeImage(request.getImage())
        );

        customer.setTrangThai(1);

        customer.setNgayTao(
                LocalDateTime.now()
        );

        customer.setNgayCapNhat(
                LocalDateTime.now()
        );

        /*
         * Lưu lần đầu để lấy ID identity.
         * DB cho phép mã khách hàng và tên tài khoản null.
         */
        customer =
                khachHangRepository.saveAndFlush(customer);

        String code =
                generateCode(customer.getId());

        customer.setMaKhachHang(code);

        /*
         * Tạm thời chưa có module login riêng,
         * username dùng bằng mã khách hàng.
         */
        customer.setTenTaiKhoan(code);

        customer =
                khachHangRepository.save(customer);

        createAddressInternal(
                customer,
                request.getDefaultAddress(),
                true
        );

        return toResponse(
                customer,
                true
        );
    }

    @Transactional
    public KhachHangResponse update(
            String code,
            KhachHangRequest request
    ) {
        KhachHang customer =
                findCustomer(code);

        validateCustomerRequest(
                request,
                customer
        );

        customer.setTenKhachHang(
                clean(request.getName())
        );

        customer.setEmail(
                cleanLower(request.getEmail())
        );

        customer.setSoDienThoai(
                cleanNullable(request.getPhone())
        );

        customer.setNgaySinh(
                request.getDob()
        );

        customer.setGioiTinh(
                request.getGender()
        );

        String image =
                normalizeImage(request.getImage());

        if (
                image != null
                        || request.getImage() == null
                        || request.getImage().isBlank()
        ) {
            customer.setHinhAnh(image);
        }

        customer.setNgayCapNhat(
                LocalDateTime.now()
        );

        customer =
                khachHangRepository.save(customer);

        /*
         * Nếu FE gửi defaultAddress thì cập nhật
         * địa chỉ mặc định hiện tại.
         */
        if (request.getDefaultAddress() != null) {

            DiaChiKhachHang currentDefault =
                    diaChiRepository
                            .findFirstByIdKhachHang_IdAndTrangThaiAndIsMacDinhTrue(
                                    customer.getId(),
                                    1
                            )
                            .orElse(null);

            if (currentDefault == null) {

                createAddressInternal(
                        customer,
                        request.getDefaultAddress(),
                        true
                );

            } else {

                updateAddressEntity(
                        customer,
                        currentDefault,
                        request.getDefaultAddress(),
                        true
                );
            }
        }

        return toResponse(
                customer,
                true
        );
    }

    @Transactional
    public KhachHangResponse updateStatus(
            String code,
            TrangThaiKhachHangRequest request
    ) {
        KhachHang customer =
                findCustomer(code);

        customer.setTrangThai(
                request.getStatus()
        );

        customer.setNgayCapNhat(
                LocalDateTime.now()
        );

        customer =
                khachHangRepository.save(customer);

        return toResponse(
                customer,
                true
        );
    }

    public List<DiaChiKhachHangResponse>
    getAddresses(String code) {

        KhachHang customer =
                findCustomer(code);

        return diaChiRepository
                .findByIdKhachHang_IdAndTrangThaiOrderByIsMacDinhDescIdDesc(
                        customer.getId(),
                        1
                )
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public DiaChiKhachHangResponse createAddress(
            String code,
            DiaChiKhachHangRequest request
    ) {
        KhachHang customer =
                findCustomer(code);

        boolean hasActive =
                !diaChiRepository
                        .findByIdKhachHang_IdAndTrangThaiOrderByIsMacDinhDescIdDesc(
                                customer.getId(),
                                1
                        )
                        .isEmpty();

        DiaChiKhachHang address =
                createAddressInternal(
                        customer,
                        request,
                        !hasActive
                                || Boolean.TRUE.equals(
                                request.getIsDefault()
                        )
                );

        return toAddressResponse(address);
    }

    @Transactional
    public DiaChiKhachHangResponse updateAddress(
            String code,
            Long addressId,
            DiaChiKhachHangRequest request
    ) {
        KhachHang customer =
                findCustomer(code);

        DiaChiKhachHang address =
                findAddress(
                        customer.getId(),
                        addressId
                );

        boolean makeDefault =
                Boolean.TRUE.equals(
                        request.getIsDefault()
                )
                        || Boolean.TRUE.equals(
                        address.getIsMacDinh()
                );

        address =
                updateAddressEntity(
                        customer,
                        address,
                        request,
                        makeDefault
                );

        return toAddressResponse(address);
    }

    @Transactional
    public DiaChiKhachHangResponse setDefaultAddress(
            String code,
            Long addressId
    ) {
        KhachHang customer =
                findCustomer(code);

        DiaChiKhachHang target =
                findAddress(
                        customer.getId(),
                        addressId
                );

        if (
                !Integer.valueOf(1)
                        .equals(target.getTrangThai())
        ) {
            throw AppException.badRequest(
                    "Địa chỉ đã ngừng hoạt động"
            );
        }

        clearDefaultAddress(
                customer.getId()
        );

        target.setIsMacDinh(true);

        target =
                diaChiRepository.save(target);

        return toAddressResponse(target);
    }

    @Transactional
    public void deleteAddress(
            String code,
            Long addressId
    ) {
        KhachHang customer =
                findCustomer(code);

        DiaChiKhachHang address =
                findAddress(
                        customer.getId(),
                        addressId
                );

        boolean wasDefault =
                Boolean.TRUE.equals(
                        address.getIsMacDinh()
                );

        /*
         * Soft delete.
         */
        address.setTrangThai(0);
        address.setIsMacDinh(false);

        diaChiRepository.save(address);

        /*
         * Nếu xóa địa chỉ mặc định,
         * tự lấy địa chỉ hoạt động gần nhất
         * làm mặc định.
         */
        if (wasDefault) {

            diaChiRepository
                    .findFirstByIdKhachHang_IdAndTrangThaiOrderByIdDesc(
                            customer.getId(),
                            1
                    )
                    .ifPresent(next -> {

                        next.setIsMacDinh(true);

                        diaChiRepository.save(next);
                    });
        }
    }

    private KhachHang findCustomer(
            String code
    ) {
        if (
                code == null
                        || code.isBlank()
        ) {
            throw AppException.badRequest(
                    "Mã khách hàng không hợp lệ"
            );
        }

        return khachHangRepository
                .findByMaKhachHang(code.trim())
                .orElseThrow(
                        () -> AppException.notFound(
                                "Không tìm thấy khách hàng "
                                        + code
                        )
                );
    }

    private DiaChiKhachHang findAddress(
            Long customerId,
            Long addressId
    ) {
        return diaChiRepository
                .findByIdAndIdKhachHang_Id(
                        addressId,
                        customerId
                )
                .orElseThrow(
                        () -> AppException.notFound(
                                "Không tìm thấy địa chỉ khách hàng"
                        )
                );
    }

    private void validateCustomerRequest(
            KhachHangRequest request,
            KhachHang currentCustomer
    ) {
        Long currentId = currentCustomer == null ? null : currentCustomer.getId();
        String email =
                cleanLower(request.getEmail());

        String phone =
                cleanNullable(request.getPhone());

        boolean emailExists;

        if (currentCustomer != null && java.util.Objects.equals(email, cleanLower(currentCustomer.getEmail()))) {
            emailExists = false;
        } else if (currentId == null) {

            emailExists =
                    khachHangRepository
                            .existsByEmailIgnoreCase(
                                    email
                            );

        } else {

            emailExists =
                    khachHangRepository
                            .existsByEmailIgnoreCaseAndIdNot(
                                    email,
                                    currentId
                            );
        }

        if (emailExists) {
            throw AppException.conflict(
                    "Email đã được sử dụng bởi khách hàng khác"
            );
        }

        if (phone != null && (currentCustomer == null
                || !java.util.Objects.equals(phone, cleanNullable(currentCustomer.getSoDienThoai())))) {

            boolean phoneExists;

            if (currentId == null) {

                phoneExists =
                        khachHangRepository
                                .existsBySoDienThoai(
                                        phone
                                );

            } else {

                phoneExists =
                        khachHangRepository
                                .existsBySoDienThoaiAndIdNot(
                                        phone,
                                        currentId
                                );
            }

            if (phoneExists) {
                throw AppException.conflict(
                        "Số điện thoại đã được sử dụng bởi khách hàng khác"
                );
            }
        }
    }

    private DiaChiKhachHang createAddressInternal(
            KhachHang customer,
            DiaChiKhachHangRequest request,
            boolean forceDefault
    ) {
        if (forceDefault) {
            clearDefaultAddress(
                    customer.getId()
            );
        }

        DiaChiKhachHang address =
                new DiaChiKhachHang();

        address.setIdKhachHang(customer);

        fillAddress(
                address,
                request
        );

        address.setIsMacDinh(
                forceDefault
                        || Boolean.TRUE.equals(
                        request.getIsDefault()
                )
        );

        address.setTrangThai(1);

        return diaChiRepository.save(address);
    }

    private DiaChiKhachHang updateAddressEntity(
            KhachHang customer,
            DiaChiKhachHang address,
            DiaChiKhachHangRequest request,
            boolean makeDefault
    ) {
        if (makeDefault) {
            clearDefaultAddress(
                    customer.getId()
            );
        }

        fillAddress(
                address,
                request
        );

        address.setIsMacDinh(
                makeDefault
        );

        address.setTrangThai(1);

        return diaChiRepository.save(address);
    }

    private void fillAddress(
            DiaChiKhachHang address,
            DiaChiKhachHangRequest request
    ) {
        address.setTenNguoiNhan(
                clean(request.getReceiverName())
        );

        address.setSdtNguoiNhan(
                clean(request.getReceiverPhone())
        );

        address.setTinhThanh(
                clean(request.getProvince())
        );

        address.setQuanHuyen(
                cleanNullable(request.getDistrict())
        );

        address.setPhuongXa(
                clean(request.getWard())
        );

        address.setDiaChiCuThe(
                clean(request.getStreet())
        );

        address.setLoaiDiaChi(
                request.getAddressType() == null
                        ? 1
                        : request.getAddressType()
        );
    }

    private void clearDefaultAddress(
            Long customerId
    ) {
        List<DiaChiKhachHang> addresses =
                diaChiRepository
                        .findByIdKhachHang_IdAndTrangThaiOrderByIsMacDinhDescIdDesc(
                                customerId,
                                1
                        );

        for (
                DiaChiKhachHang address
                : addresses
        ) {
            if (
                    Boolean.TRUE.equals(
                            address.getIsMacDinh()
                    )
            ) {
                address.setIsMacDinh(false);

                diaChiRepository.save(address);
            }
        }
    }

    private KhachHangResponse toResponse(
            KhachHang customer,
            boolean includeAddresses
    ) {
        List<DiaChiKhachHang> entities =
                diaChiRepository
                        .findByIdKhachHang_IdAndTrangThaiOrderByIsMacDinhDescIdDesc(
                                customer.getId(),
                                1
                        );

        DiaChiKhachHangResponse defaultAddress =
                entities
                        .stream()
                        .filter(
                                a -> Boolean.TRUE.equals(
                                        a.getIsMacDinh()
                                )
                        )
                        .findFirst()
                        .map(
                                this::toAddressResponse
                        )
                        .orElse(null);

        List<DiaChiKhachHangResponse> addresses =
                includeAddresses
                        ? entities
                        .stream()
                        .map(
                                this::toAddressResponse
                        )
                        .toList()
                        : null;

        return KhachHangResponse
                .builder()
                .id(customer.getId())
                .code(customer.getMaKhachHang())
                .username(customer.getTenTaiKhoan())
                .name(customer.getTenKhachHang())
                .email(customer.getEmail())
                .phone(customer.getSoDienThoai())
                .dob(customer.getNgaySinh())
                .gender(customer.getGioiTinh())
                .genderLabel(
                        genderLabel(
                                customer.getGioiTinh()
                        )
                )
                .image(customer.getHinhAnh())
                .status(customer.getTrangThai())
                .statusLabel(
                        Integer.valueOf(1)
                                .equals(
                                        customer.getTrangThai()
                                )
                                ? "Hoạt động"
                                : "Đã khóa"
                )
                .active(
                        Integer.valueOf(1)
                                .equals(
                                        customer.getTrangThai()
                                )
                )
                .createdAt(customer.getNgayTao())
                .updatedAt(customer.getNgayCapNhat())
                .defaultAddress(defaultAddress)
                .addresses(addresses)
                .build();
    }

    private DiaChiKhachHangResponse toAddressResponse(
            DiaChiKhachHang address
    ) {
        return DiaChiKhachHangResponse
                .builder()
                .id(address.getId())
                .receiverName(
                        address.getTenNguoiNhan()
                )
                .receiverPhone(
                        address.getSdtNguoiNhan()
                )
                .province(
                        address.getTinhThanh()
                )
                .district(
                        address.getQuanHuyen()
                )
                .ward(
                        address.getPhuongXa()
                )
                .street(
                        address.getDiaChiCuThe()
                )
                .addressType(
                        address.getLoaiDiaChi()
                )
                .addressTypeLabel(
                        Integer.valueOf(2)
                                .equals(
                                        address.getLoaiDiaChi()
                                )
                                ? "Văn phòng"
                                : "Nhà riêng"
                )
                .isDefault(
                        Boolean.TRUE.equals(
                                address.getIsMacDinh()
                        )
                )
                .status(
                        address.getTrangThai()
                )
                .fullAddress(
                        buildFullAddress(address)
                )
                .build();
    }

    private String buildFullAddress(
            DiaChiKhachHang address
    ) {
        List<String> parts =
                new ArrayList<>();

        addPart(
                parts,
                address.getDiaChiCuThe()
        );

        addPart(
                parts,
                address.getPhuongXa()
        );

        addPart(
                parts,
                address.getQuanHuyen()
        );

        addPart(
                parts,
                address.getTinhThanh()
        );

        return String.join(
                ", ",
                parts
        );
    }

    private void addPart(
            List<String> parts,
            String value
    ) {
        if (
                value != null
                        && !value.isBlank()
        ) {
            parts.add(value.trim());
        }
    }

    private String generateCode(
            Long id
    ) {
        long candidate = id;
        String code = String.format(Locale.ROOT, "KH%04d", candidate);
        while (khachHangRepository.existsByMaKhachHang(code)
                || khachHangRepository.existsByTenTaiKhoan(code)) {
            if (candidate == Long.MAX_VALUE) {
                throw AppException.conflict("Không thể sinh mã khách hàng mới");
            }
            code = String.format(Locale.ROOT, "KH%04d", ++candidate);
        }
        return code;
    }

    private String genderLabel(
            Integer gender
    ) {
        if (gender == null) {
            return "Chưa cập nhật";
        }

        return switch (gender) {
            case 1 -> "Nam";
            case 2 -> "Nữ";
            default -> "Khác";
        };
    }

    private String normalizeKeyword(
            String value
    ) {
        return value == null
                ? null
                : value.trim();
    }

    private String clean(
            String value
    ) {
        return value == null
                ? null
                : value.trim();
    }

    private String cleanLower(
            String value
    ) {
        String cleaned =
                clean(value);

        return cleaned == null
                ? null
                : cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanNullable(
            String value
    ) {
        String cleaned =
                clean(value);

        return cleaned == null
                || cleaned.isBlank()
                ? null
                : cleaned;
    }

    private String normalizeImage(
            String image
    ) {
        if (
                image == null
                        || image.isBlank()
        ) {
            return null;
        }

        String value =
                image.trim();

        // The upload API returns a persisted path; raw image data is never stored in NVARCHAR.
        if (
                value.startsWith(
                        "data:image/"
                )
        ) {
            throw AppException.badRequest("Hãy tải ảnh đại diện lên trước khi lưu khách hàng");
        }

        if (value.length() > 1000) {
            throw AppException.badRequest(
                    "Đường dẫn ảnh tối đa 1000 ký tự"
            );
        }

        return value;
    }

    private void validateStatusFilter(
            Integer status
    ) {
        if (
                status != null
                        && status != 0
                        && status != 1
        ) {
            throw AppException.badRequest(
                    "Trạng thái chỉ nhận 0 hoặc 1"
            );
        }
    }
}
