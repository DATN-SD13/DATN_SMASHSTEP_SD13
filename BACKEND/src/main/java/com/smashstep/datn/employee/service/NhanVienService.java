package com.smashstep.datn.employee.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.dto.NhanVienResponse;
import com.smashstep.datn.employee.dto.TrangThaiNhanVienRequest;
import com.smashstep.datn.employee.dto.VaiTroResponse;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.mail.EmployeeMailService;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.employee.repository.VaiTroRepository;
import com.smashstep.datn.employee.util.PasswordGenerator;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;

    /*
     * Service gửi email.
     */
    private final EmployeeMailService employeeMailService;

    /*
     * BCrypt password encoder.
     */
    private final PasswordEncoder passwordEncoder;


    // =========================================================
    // DANH SÁCH + TÌM KIẾM + LỌC + PHÂN TRANG
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<NhanVienResponse> layDanhSach(
            String tuKhoa,
            Long vaiTroId,
            Integer trangThai,
            int page,
            int size
    ) {

        if (page < 1) {
            throw AppException.badRequest(
                    "Page phải lớn hơn hoặc bằng 1"
            );
        }

        if (size < 1) {
            throw AppException.badRequest(
                    "Size phải lớn hơn hoặc bằng 1"
            );
        }

        validateTrangThaiNullable(
                trangThai
        );

        Pageable pageable =
                PageRequest.of(
                        page - 1,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "id"
                        )
                );

        String keyword =
                normalizeNullable(
                        tuKhoa
                );

        Page<NhanVienResponse> result =
                nhanVienRepository
                        .timKiem(
                                keyword,
                                vaiTroId,
                                trangThai,
                                pageable
                        )
                        .map(
                                this::toResponse
                        );

        return PageResponse.from(
                result
        );
    }


    // =========================================================
    // CHI TIẾT
    // =========================================================

    @Transactional(readOnly = true)
    public NhanVienResponse layChiTiet(
            String ma
    ) {

        return toResponse(
                findByMa(ma)
        );
    }


    // =========================================================
    // THÊM NHÂN VIÊN
    // =========================================================

    @Transactional
    public NhanVienResponse them(
            NhanVienRequest request
    ) {

        /*
         * ============================
         * 1. VALIDATE
         * ============================
         */

        validateRequest(
                request
        );

        String email =
                request
                        .getEmail()
                        .trim();

        /*
         * Kiểm tra email đã tồn tại.
         */
        if (
                nhanVienRepository
                        .existsByEmailIgnoreCase(
                                email
                        )
        ) {

            throw AppException.conflict(
                    "Email đã được sử dụng"
            );
        }


        /*
         * ============================
         * 2. LẤY VAI TRÒ
         * ============================
         */

        VaiTro vaiTro =
                findRole(
                        request.getRoleId()
                );


        /*
         * ============================
         * 3. TẠO ENTITY
         * ============================
         */

        NhanVien nhanVien =
                new NhanVien();

        nhanVien.setIdVaiTro(
                vaiTro
        );

        /*
         * Chưa có ID nên chưa sinh mã.
         */
        nhanVien.setMaNhanVien(
                null
        );

        nhanVien.setTenDangNhap(
                null
        );

        nhanVien.setTenNhanVien(
                request
                        .getName()
                        .trim()
        );

        nhanVien.setEmail(
                email
        );

        nhanVien.setSoDienThoai(
                request
                        .getPhone()
                        .trim()
        );

        nhanVien.setGioiTinh(
                request.getGender()
        );

        nhanVien.setNgaySinh(
                request.getDob()
        );

        nhanVien.setDiaChi(
                normalizeNullable(
                        request.getStreet()
                )
        );

        nhanVien.setPhuongXa(
                normalizeNullable(
                        request.getWard()
                )
        );

        nhanVien.setTinhThanh(
                normalizeNullable(
                        request.getProvince()
                )
        );

        /*
         * Avatar FE hiện đang gửi base64.
         * Không lưu base64 vào NVARCHAR(1000).
         */
        nhanVien.setHinhAnh(
                normalizeImage(
                        request.getImage()
                )
        );

        /*
         * Mặc định hoạt động.
         */
        nhanVien.setTrangThai(
                1
        );

        LocalDateTime now =
                LocalDateTime.now();

        nhanVien.setNgayTao(
                now
        );

        nhanVien.setNgayCapNhat(
                now
        );

        /*
         * Lần save đầu tiên chưa cần password.
         */
        nhanVien.setMatKhau(
                null
        );


        /*
         * ============================
         * 4. SAVE LẦN 1
         * ============================
         *
         * SQL Server sinh IDENTITY.
         */

        NhanVien saved =
                nhanVienRepository
                        .saveAndFlush(
                                nhanVien
                        );


        /*
         * ============================
         * 5. SINH MÃ NHÂN VIÊN
         * ============================
         *
         * ID 1   -> NV0001
         * ID 12  -> NV0012
         * ID 123 -> NV0123
         */

        String maNhanVien =
                generateEmployeeCode(
                        saved.getId()
                );

        if (
                nhanVienRepository
                        .existsByMaNhanVien(
                                maNhanVien
                        )
        ) {

            throw AppException.conflict(
                    "Mã nhân viên "
                            + maNhanVien
                            + " đã tồn tại"
            );
        }

        saved.setMaNhanVien(
                maNhanVien
        );

        /*
         * Username hiện tại
         * = mã nhân viên.
         */
        saved.setTenDangNhap(
                maNhanVien
        );


        /*
         * ============================
         * 6. SINH TÀI KHOẢN ĐĂNG NHẬP
         * ============================
         *
         * Mọi nhân viên được tạo mới đều được cấp:
         * - username = mã nhân viên
         * - mật khẩu tạm thời ngẫu nhiên
         * - mật khẩu trong DB được mã hóa BCrypt
         */

        String temporaryPassword =
                PasswordGenerator.generate();

        saved.setMatKhau(
                passwordEncoder.encode(
                        temporaryPassword
                )
        );

        saved.setNgayCapNhat(
                LocalDateTime.now()
        );

        saved = nhanVienRepository.saveAndFlush(
                saved
        );


        /*
         * ============================
         * 7. GỬI EMAIL TÀI KHOẢN
         * ============================
         *
         * Nếu gửi email thất bại thì ném exception để
         * transaction rollback, tránh tạo tài khoản mà
         * nhân viên không nhận được thông tin đăng nhập.
         */

        try {
            employeeMailService.sendEmployeeAccountEmail(
                    saved.getEmail(),
                    saved.getTenNhanVien(),

                    saved.getIdVaiTro() != null
                            ? saved.getIdVaiTro().getTenVaiTro()
                            : "Nhân viên",

                    saved.getTenDangNhap(),
                    temporaryPassword
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Không thể gửi email thông tin tài khoản tới "
                            + saved.getEmail()
                            + ". Nhân viên chưa được tạo. Vui lòng kiểm tra cấu hình email và thử lại.",
                    e
            );
        }


        /*
         * ============================
         * 10. RESPONSE
         * ============================
         */

        return toResponse(
                saved
        );
    }


    // =========================================================
    // SỬA NHÂN VIÊN
    // =========================================================

    @Transactional
    public NhanVienResponse sua(
            String ma,
            NhanVienRequest request
    ) {

        NhanVien nhanVien =
                findByMa(
                        ma
                );

        validateRequest(
                request
        );

        String email =
                request
                        .getEmail()
                        .trim();

        /*
         * Email không được trùng
         * với nhân viên khác.
         */
        if (
                nhanVienRepository
                        .existsByEmailIgnoreCaseAndIdNot(
                                email,
                                nhanVien.getId()
                        )
        ) {

            throw AppException.conflict(
                    "Email đã được sử dụng"
            );
        }


        /*
         * Vai trò mới.
         */
        VaiTro vaiTro =
                findRole(
                        request.getRoleId()
                );

        nhanVien.setIdVaiTro(
                vaiTro
        );

        nhanVien.setTenNhanVien(
                request
                        .getName()
                        .trim()
        );

        nhanVien.setEmail(
                email
        );

        nhanVien.setSoDienThoai(
                request
                        .getPhone()
                        .trim()
        );

        nhanVien.setGioiTinh(
                request.getGender()
        );

        nhanVien.setNgaySinh(
                request.getDob()
        );

        nhanVien.setDiaChi(
                normalizeNullable(
                        request.getStreet()
                )
        );

        nhanVien.setPhuongXa(
                normalizeNullable(
                        request.getWard()
                )
        );

        nhanVien.setTinhThanh(
                normalizeNullable(
                        request.getProvince()
                )
        );


        /*
         * Chỉ cập nhật ảnh khi có ảnh mới.
         */
        String image =
                normalizeImage(
                        request.getImage()
                );

        if (image != null) {

            nhanVien.setHinhAnh(
                    image
            );
        }


        nhanVien.setNgayCapNhat(
                LocalDateTime.now()
        );


        return toResponse(
                nhanVienRepository
                        .save(
                                nhanVien
                        )
        );
    }


    // =========================================================
    // KHÓA / MỞ KHÓA
    // =========================================================

    @Transactional
    public NhanVienResponse capNhatTrangThai(
            String ma,
            TrangThaiNhanVienRequest request
    ) {

        if (
                request == null
                        ||
                        request.getStatus() == null
        ) {

            throw AppException.badRequest(
                    "Trạng thái không được để trống"
            );
        }


        validateTrangThai(
                request.getStatus()
        );


        NhanVien nhanVien =
                findByMa(
                        ma
                );


        nhanVien.setTrangThai(
                request.getStatus()
        );

        nhanVien.setNgayCapNhat(
                LocalDateTime.now()
        );


        return toResponse(
                nhanVienRepository
                        .save(
                                nhanVien
                        )
        );
    }


    // =========================================================
    // DANH SÁCH VAI TRÒ
    // =========================================================

    @Transactional(readOnly = true)
    public List<VaiTroResponse> layDanhSachVaiTro() {

        return vaiTroRepository
                .findByTrangThaiOrderByIdAsc(
                        1
                )
                .stream()
                .map(
                        vaiTro ->
                                new VaiTroResponse(
                                        vaiTro.getId(),
                                        vaiTro.getTenVaiTro()
                                )
                )
                .toList();
    }


    // =========================================================
    // FIND EMPLOYEE
    // =========================================================

    private NhanVien findByMa(
            String ma
    ) {

        if (
                ma == null
                        ||
                        ma.isBlank()
        ) {

            throw AppException.badRequest(
                    "Mã nhân viên không được để trống"
            );
        }


        String employeeCode =
                ma.trim();


        return nhanVienRepository
                .findByMaNhanVien(
                        employeeCode
                )
                .orElseThrow(
                        () ->
                                AppException.notFound(
                                        "Không tìm thấy nhân viên có mã "
                                                + employeeCode
                                )
                );
    }


    // =========================================================
    // FIND ROLE
    // =========================================================

    private VaiTro findRole(
            Long roleId
    ) {

        if (roleId == null) {

            throw AppException.badRequest(
                    "Vai trò không được để trống"
            );
        }


        VaiTro vaiTro =
                vaiTroRepository
                        .findById(
                                roleId
                        )
                        .orElseThrow(
                                () ->
                                        AppException.notFound(
                                                "Không tìm thấy vai trò"
                                        )
                        );


        if (
                vaiTro.getTrangThai() == null
                        ||
                        vaiTro.getTrangThai() != 1
        ) {

            throw AppException.badRequest(
                    "Vai trò đang ngừng hoạt động"
            );
        }


        return vaiTro;
    }


    // =========================================================
    // VALIDATE REQUEST
    // =========================================================

    private void validateRequest(
            NhanVienRequest request
    ) {

        if (request == null) {

            throw AppException.badRequest(
                    "Dữ liệu nhân viên không hợp lệ"
            );
        }


        validateGender(
                request.getGender()
        );


        if (request.getDob() != null) {

            LocalDate today = LocalDate.now();
            LocalDate latestAllowedDob = today.minusYears(18);

            if (request.getDob().isAfter(today)) {
                throw AppException.badRequest(
                        "Ngày sinh không được lớn hơn ngày hiện tại"
                );
            }

            /*
             * Nhân viên phải đủ 18 tuổi trở lên.
             * Ví dụ hôm nay 16/10/2026 thì ngày sinh
             * phải <= 16/10/2008.
             */
            if (request.getDob().isAfter(latestAllowedDob)) {
                throw AppException.badRequest(
                        "Nhân viên phải đủ 18 tuổi trở lên"
                );
            }
        }
    }


    // =========================================================
    // VALIDATE GENDER
    // =========================================================

    private void validateGender(
            Integer gender
    ) {

        if (gender == null) {

            throw AppException.badRequest(
                    "Giới tính không được để trống"
            );
        }


        if (
                gender != 0
                        &&
                        gender != 1
                        &&
                        gender != 2
        ) {

            throw AppException.badRequest(
                    "Giới tính chỉ nhận 0, 1 hoặc 2"
            );
        }
    }


    // =========================================================
    // VALIDATE STATUS
    // =========================================================

    private void validateTrangThai(
            Integer status
    ) {

        if (
                status == null
                        ||
                        (
                                status != 0
                                        &&
                                        status != 1
                        )
        ) {

            throw AppException.badRequest(
                    "Trạng thái chỉ nhận 0 hoặc 1"
            );
        }
    }


    private void validateTrangThaiNullable(
            Integer status
    ) {

        if (status != null) {

            validateTrangThai(
                    status
            );
        }
    }


    // =========================================================
    // GENERATE CODE
    // =========================================================

    private String generateEmployeeCode(
            Long id
    ) {

        return String.format(
                "NV%04d",
                id
        );
    }


    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private NhanVienResponse toResponse(
            NhanVien nv
    ) {

        Long roleId =
                null;

        String roleName =
                null;


        if (
                nv.getIdVaiTro() != null
        ) {

            roleId =
                    nv
                            .getIdVaiTro()
                            .getId();

            roleName =
                    nv
                            .getIdVaiTro()
                            .getTenVaiTro();
        }


        return NhanVienResponse
                .builder()

                .id(
                        nv.getId()
                )

                .code(
                        nv.getMaNhanVien()
                )

                .username(
                        nv.getTenDangNhap()
                )

                .name(
                        nv.getTenNhanVien()
                )

                .email(
                        nv.getEmail()
                )

                .phone(
                        nv.getSoDienThoai()
                )

                .gender(
                        nv.getGioiTinh()
                )

                .genderLabel(
                        getGenderLabel(
                                nv.getGioiTinh()
                        )
                )

                .dob(
                        nv.getNgaySinh()
                )

                .street(
                        nv.getDiaChi()
                )

                .ward(
                        nv.getPhuongXa()
                )

                .province(
                        nv.getTinhThanh()
                )

                .address(
                        buildAddress(
                                nv
                        )
                )

                .roleId(
                        roleId
                )

                .role(
                        roleName
                )

                .status(
                        nv.getTrangThai()
                )

                .statusLabel(
                        getStatusLabel(
                                nv.getTrangThai()
                        )
                )

                .active(
                        nv.getTrangThai() != null
                                &&
                                nv.getTrangThai() == 1
                )

                .image(
                        nv.getHinhAnh()
                )

                .createdAt(
                        nv.getNgayTao()
                )

                .updatedAt(
                        nv.getNgayCapNhat()
                )

                .build();
    }


    // =========================================================
    // LABEL GIỚI TÍNH
    // =========================================================

    private String getGenderLabel(
            Integer gender
    ) {

        if (gender == null) {
            return null;
        }


        return switch (gender) {

            case 1 ->
                    "Nam";

            case 2 ->
                    "Nữ";

            case 0 ->
                    "Khác";

            default ->
                    "Không xác định";
        };
    }


    // =========================================================
    // LABEL TRẠNG THÁI
    // =========================================================

    private String getStatusLabel(
            Integer status
    ) {

        if (status == null) {
            return null;
        }


        return switch (status) {

            case 1 ->
                    "Hoạt động";

            case 0 ->
                    "Ngừng hoạt động";

            default ->
                    "Không xác định";
        };
    }


    // =========================================================
    // BUILD ADDRESS
    // =========================================================

    private String buildAddress(
            NhanVien nv
    ) {

        StringBuilder address =
                new StringBuilder();


        appendAddress(
                address,
                nv.getDiaChi()
        );


        appendAddress(
                address,
                nv.getPhuongXa()
        );


        appendAddress(
                address,
                nv.getTinhThanh()
        );


        return address.toString();
    }


    private void appendAddress(
            StringBuilder address,
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return;
        }


        if (
                address.length() > 0
        ) {

            address.append(
                    ", "
            );
        }


        address.append(
                value.trim()
        );
    }


    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String result =
                value.trim();


        return result.isEmpty()
                ? null
                : result;
    }


    // =========================================================
    // NORMALIZE IMAGE
    // =========================================================

    private String normalizeImage(
            String image
    ) {

        String result =
                normalizeNullable(
                        image
                );


        if (result == null) {
            return null;
        }


        /*
         * EmployeeForm hiện đọc ảnh
         * thành base64.
         *
         * Không lưu base64 vào
         * hinh_anh NVARCHAR(1000).
         */
        if (
                result.startsWith(
                        "data:image/"
                )
        ) {

            return null;
        }


        if (
                result.length() > 1000
        ) {

            throw AppException.badRequest(
                    "Đường dẫn hình ảnh quá dài"
            );
        }


        return result;
    }
}