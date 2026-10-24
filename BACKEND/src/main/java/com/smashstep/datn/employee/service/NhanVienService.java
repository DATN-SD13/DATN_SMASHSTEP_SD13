package com.smashstep.datn.employee.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.dto.DoiMatKhauRequest;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.dto.NhanVienResponse;
import com.smashstep.datn.employee.dto.TrangThaiNhanVienRequest;
import com.smashstep.datn.employee.dto.VaiTroResponse;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.repository.NhanVienRepository;
import com.smashstep.datn.employee.repository.VaiTroRepository;
import com.smashstep.datn.employee.mail.EmployeeMailService;
import com.smashstep.datn.employee.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanVienService {

    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;
    private final EmployeeMailService employeeMailService;
    private final PasswordEncoder passwordEncoder;
    @org.springframework.beans.factory.annotation.Autowired private NhanVienWriteLock employeeWriteLock;

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
            throw AppException.badRequest("Page phải lớn hơn hoặc bằng 1");
        }

        if (size < 1) {
            throw AppException.badRequest("Size phải lớn hơn hoặc bằng 1");
        }

        validateTrangThaiNullable(trangThai);

        if ((long) (page - 1) * (Math.min(size, 100)) > Integer.MAX_VALUE) throw AppException.badRequest("Trang yêu cầu vượt giới hạn phân trang");
        Pageable pageable = PageRequest.of(
                page - 1,
                Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "id")
        );

        String keyword = normalizeNullable(tuKhoa);

        Page<NhanVienResponse> result = nhanVienRepository
                .timKiem(
                        keyword,
                        vaiTroId,
                        trangThai,
                        pageable
                )
                .map(this::toResponse);

        return PageResponse.from(result);
    }

    // =========================================================
    // CHI TIẾT
    // =========================================================
    @Transactional(readOnly = true)
    public NhanVienResponse layChiTiet(String ma) {
        return toResponse(findByMa(ma));
    }

    // =========================================================
    // THÊM NHÂN VIÊN
    // =========================================================
    @Transactional
    public NhanVienResponse them(NhanVienRequest request) {
        lockEmployeeWrites();
        return createAccount(request, this::sendEmployeeAccountAfterCommit);
    }

    /** Same creation path, but persist mail jobs in the enclosing import transaction. */
    @Transactional
    public NhanVienResponse themTuExcel(NhanVienRequest request, java.util.function.BiConsumer<NhanVien, String> queueMail) {
        lockEmployeeWrites();
        return createAccount(request, queueMail);
    }

    public void validateImportRules(NhanVienRequest request) { validateRequest(request); normalizeImage(request.getImage()); }

    private void lockEmployeeWrites() { if (employeeWriteLock != null) employeeWriteLock.lock(); }

    private NhanVienResponse createAccount(NhanVienRequest request, java.util.function.BiConsumer<NhanVien, String> accountReady) {
        validateRequest(request);

        String email = request.getEmail().trim();

        if (nhanVienRepository.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("Email đã được sử dụng");
        }

        VaiTro vaiTro = findRole(request.getRoleId());

        NhanVien nhanVien = new NhanVien();

        nhanVien.setIdVaiTro(vaiTro);

        /*
         * Chưa sinh mã ở đây.
         * Ta lưu lần đầu để SQL Server cấp IDENTITY id.
         */
        nhanVien.setMaNhanVien(null);
        nhanVien.setTenDangNhap(null);

        nhanVien.setTenNhanVien(request.getName().trim());
        nhanVien.setEmail(email);
        nhanVien.setSoDienThoai(request.getPhone().trim());
        nhanVien.setGioiTinh(request.getGender());
        nhanVien.setNgaySinh(request.getDob());

        nhanVien.setDiaChi(normalizeNullable(request.getStreet()));
        nhanVien.setPhuongXa(normalizeNullable(request.getWard()));
        nhanVien.setTinhThanh(normalizeNullable(request.getProvince()));

        /*
         * Không lưu mật khẩu plaintext.
         * Module authentication chưa được thống nhất.
         */
        nhanVien.setMatKhau(null);

        /*
         * Avatar hiện tại FE đọc file thành base64.
         * Không lưu base64 vào NVARCHAR(1000).
         */
        nhanVien.setHinhAnh(normalizeImage(request.getImage()));

        nhanVien.setTrangThai(1);
        nhanVien.setNgayTao(LocalDateTime.now());
        nhanVien.setNgayCapNhat(LocalDateTime.now());

        /*
         * saveAndFlush để lấy ID do database sinh.
         */
        NhanVien saved = nhanVienRepository.saveAndFlush(nhanVien);

        /*
         * Mã nhân viên:
         * id = 1   -> NV0001
         * id = 12  -> NV0012
         * id = 123 -> NV0123
         */
        String maNhanVien = generateEmployeeCode(saved.getId());

        if (nhanVienRepository.existsByMaNhanVien(maNhanVien)) {
            throw AppException.conflict(
                    "Mã nhân viên " + maNhanVien + " đã tồn tại"
            );
        }

        saved.setMaNhanVien(maNhanVien);

        /*
         * Tạm thời username = mã nhân viên.
         * Khi module đăng nhập hoàn thiện có thể thay đổi chính sách này.
         */
        saved.setTenDangNhap(maNhanVien);

        // All newly created employees receive their own BCrypt-protected account.
        String temporaryPassword = PasswordGenerator.generate();
        saved.setMatKhau(passwordEncoder.encode(temporaryPassword));
        saved.setNgayCapNhat(LocalDateTime.now());
        saved = nhanVienRepository.save(saved);
        accountReady.accept(saved, temporaryPassword);

        return toResponse(saved);
    }

    // =========================================================
    // SỬA NHÂN VIÊN
    // =========================================================
    /** Hồ sơ cá nhân không phải API thay đổi vai trò, kể cả form cũ đang giữ roleId cũ. */
    @Transactional
    public NhanVienResponse suaThongTinCaNhan(String ma, NhanVienRequest request) {
        lockEmployeeWrites();
        var current = findByMa(ma);
        request.setRoleId(current.getIdVaiTro() == null ? null : current.getIdVaiTro().getId());
        return sua(ma, request);
    }

    @Transactional
    public NhanVienResponse sua(
            String ma,
            NhanVienRequest request
    ) {

        lockEmployeeWrites();
        NhanVien nhanVien = findByMa(ma);

        validateRequest(request);

        String email = request.getEmail().trim();

        if (nhanVienRepository.existsByEmailIgnoreCaseAndIdNot(
                email,
                nhanVien.getId()
        )) {
            throw AppException.conflict("Email đã được sử dụng");
        }

        VaiTro vaiTro = findRole(request.getRoleId());

        nhanVien.setIdVaiTro(vaiTro);
        nhanVien.setTenNhanVien(request.getName().trim());
        nhanVien.setEmail(email);
        nhanVien.setSoDienThoai(request.getPhone().trim());

        nhanVien.setGioiTinh(request.getGender());
        nhanVien.setNgaySinh(request.getDob());

        nhanVien.setDiaChi(
                normalizeNullable(request.getStreet())
        );

        nhanVien.setPhuongXa(
                normalizeNullable(request.getWard())
        );

        nhanVien.setTinhThanh(
                normalizeNullable(request.getProvince())
        );

        String image = normalizeImage(request.getImage());

        if (image != null) {
            nhanVien.setHinhAnh(image);
        }

        nhanVien.setNgayCapNhat(LocalDateTime.now());

        return toResponse(
                nhanVienRepository.save(nhanVien)
        );
    }

    // =========================================================
    // ĐỔI MẬT KHẨU (nhân viên tự đổi)
    // =========================================================
    @org.springframework.transaction.annotation.Transactional
    public void doiMatKhau(String username, DoiMatKhauRequest request) {
        NhanVien nv = findByMa(username);

        if (nv.getMatKhau() == null) {
            throw AppException.badRequest("Tài khoản chưa có mật khẩu. Vui lòng liên hệ quản trị viên.");
        }

        if (!passwordEncoder.matches(request.getMatKhauCu(), nv.getMatKhau())) {
            throw AppException.badRequest("Mật khẩu hiện tại không đúng.");
        }

        if (request.getMatKhauMoi().equals(request.getMatKhauCu())) {
            throw AppException.badRequest("Mật khẩu mới không được trùng mật khẩu cũ.");
        }

        nv.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        nv.setNgayCapNhat(java.time.LocalDateTime.now());
        nhanVienRepository.save(nv);
    }

    // =========================================================
    // KHÓA / MỞ KHÓA
    // =========================================================
    @Transactional
    public NhanVienResponse capNhatTrangThai(
            String ma,
            TrangThaiNhanVienRequest request
    ) {

        if (request == null || request.getStatus() == null) {
            throw AppException.badRequest(
                    "Trạng thái không được để trống"
            );
        }

        validateTrangThai(request.getStatus());

        NhanVien nhanVien = findByMa(ma);

        // Không cho phép admin tự khóa chính mình
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication == null ? null : authentication.getName();
        boolean sameAccount = authentication != null
                && authentication.getDetails() instanceof com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails actor
                && "NHAN_VIEN".equals(actor.userType()) && java.util.Objects.equals(nhanVien.getId(),actor.userId());
        if ((sameAccount || nhanVien.getMaNhanVien() != null && nhanVien.getMaNhanVien().equalsIgnoreCase(currentUsername))
                && Integer.valueOf(0).equals(request.getStatus())) {
            throw AppException.badRequest("Không thể tự khóa tài khoản của chính mình");
        }

        nhanVien.setTrangThai(request.getStatus());
        nhanVien.setNgayCapNhat(LocalDateTime.now());

        return toResponse(
                nhanVienRepository.save(nhanVien)
        );
    }

    // =========================================================
    // DANH SÁCH VAI TRÒ
    // =========================================================
    @Transactional(readOnly = true)
    public List<VaiTroResponse> layDanhSachVaiTro() {

        return vaiTroRepository
                .findByTrangThaiOrderByIdAsc(1)
                .stream()
                .map(vaiTro ->
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
    private boolean isManager(VaiTro role) {
        String name = normalizeNullable(role.getTenVaiTro());
        return name != null && (name.equalsIgnoreCase("Quản lý")
                || name.equalsIgnoreCase("Quan ly") || name.equalsIgnoreCase("Manager"));
    }

    private void sendEmployeeAccountAfterCommit(NhanVien employee, String temporaryPassword) {
        String email = employee.getEmail();
        String name = employee.getTenNhanVien();
        String username = employee.getTenDangNhap();
        String roleName = employee.getIdVaiTro() == null ? "Nhân viên" : employee.getIdVaiTro().getTenVaiTro();
        boolean manager = employee.getIdVaiTro() != null && isManager(employee.getIdVaiTro());
        Runnable send = () -> {
            try {
                if (manager) employeeMailService.sendManagerAccountEmail(email, name, username, temporaryPassword);
                else employeeMailService.sendEmployeeAccountEmail(email, name, roleName, username, temporaryPassword);
            } catch (RuntimeException ex) {
                log.warn("Đã tạo tài khoản {} nhưng gửi email thất bại ({})", username,
                        ex.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }

    private NhanVien findByMa(String ma) {

        if (ma == null || ma.isBlank()) {
            throw AppException.badRequest(
                    "Mã nhân viên không được để trống"
            );
        }

        String employeeCode = ma.trim();

        return nhanVienRepository
                .findByMaNhanVien(employeeCode)
                .orElseThrow(() ->
                        AppException.notFound(
                                "Không tìm thấy nhân viên có mã "
                                        + employeeCode
                        )
                );
    }

    // =========================================================
    // FIND ROLE
    // =========================================================
    private VaiTro findRole(Long roleId) {

        if (roleId == null) {
            throw AppException.badRequest(
                    "Vai trò không được để trống"
            );
        }

        VaiTro vaiTro = vaiTroRepository
                .findById(roleId)
                .orElseThrow(() ->
                        AppException.notFound(
                                "Không tìm thấy vai trò"
                        )
                );

        if (vaiTro.getTrangThai() == null
                || vaiTro.getTrangThai() != 1) {

            throw AppException.badRequest(
                    "Vai trò đang ngừng hoạt động"
            );
        }

        return vaiTro;
    }

    // =========================================================
    // VALIDATE REQUEST
    // =========================================================
    private void validateRequest(NhanVienRequest request) {

        if (request == null) {
            throw AppException.badRequest(
                    "Dữ liệu nhân viên không hợp lệ"
            );
        }

        validateGender(request.getGender());

        if (request.getDob() != null) {
            LocalDate today = LocalDate.now();
            if (request.getDob().isAfter(today)) {
                throw AppException.badRequest("Ngày sinh không được lớn hơn ngày hiện tại");
            }
            if (request.getDob().isAfter(today.minusYears(18))) {
                throw AppException.badRequest("Nhân viên phải đủ 18 tuổi trở lên");
            }
        }
    }

    private void validateGender(Integer gender) {

        if (gender == null) {
            throw AppException.badRequest(
                    "Giới tính không được để trống"
            );
        }

        if (gender != 0
                && gender != 1
                && gender != 2) {

            throw AppException.badRequest(
                    "Giới tính chỉ nhận 0, 1 hoặc 2"
            );
        }
    }

    private void validateTrangThai(Integer status) {

        if (status == null
                || (status != 0 && status != 1)) {

            throw AppException.badRequest(
                    "Trạng thái chỉ nhận 0 hoặc 1"
            );
        }
    }

    private void validateTrangThaiNullable(Integer status) {

        if (status != null) {
            validateTrangThai(status);
        }
    }

    // =========================================================
    // GENERATE CODE
    // =========================================================
    private String generateEmployeeCode(Long id) {
        long candidate = id;
        String code = String.format(Locale.ROOT, "NV%04d", candidate);
        while (nhanVienRepository.existsByMaNhanVien(code)
                || nhanVienRepository.existsByTenDangNhap(code)) {
            if (candidate == Long.MAX_VALUE) {
                throw AppException.conflict("Không thể sinh mã nhân viên mới");
            }
            code = String.format(Locale.ROOT, "NV%04d", ++candidate);
        }
        return code;
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================
    private NhanVienResponse toResponse(NhanVien nv) {

        Long roleId = null;
        String roleName = null;

        if (nv.getIdVaiTro() != null) {
            roleId = nv.getIdVaiTro().getId();
            roleName = nv.getIdVaiTro().getTenVaiTro();
        }

        return NhanVienResponse.builder()
                .id(nv.getId())

                .code(nv.getMaNhanVien())

                .username(nv.getTenDangNhap())

                .name(nv.getTenNhanVien())

                .email(nv.getEmail())

                .phone(nv.getSoDienThoai())

                .gender(nv.getGioiTinh())

                .genderLabel(
                        getGenderLabel(nv.getGioiTinh())
                )

                .dob(nv.getNgaySinh())

                .street(nv.getDiaChi())

                .ward(nv.getPhuongXa())

                .province(nv.getTinhThanh())

                .address(buildAddress(nv))

                .roleId(roleId)

                .role(roleName)

                .status(nv.getTrangThai())

                .statusLabel(
                        getStatusLabel(nv.getTrangThai())
                )

                .active(
                        nv.getTrangThai() != null
                                && nv.getTrangThai() == 1
                )

                .image(nv.getHinhAnh())

                .createdAt(nv.getNgayTao())

                .updatedAt(nv.getNgayCapNhat())

                .build();
    }

    // =========================================================
    // LABEL
    // =========================================================
    private String getGenderLabel(Integer gender) {

        if (gender == null) {
            return null;
        }

        return switch (gender) {
            case 1 -> "Nam";
            case 2 -> "Nữ";
            case 0 -> "Khác";
            default -> "Không xác định";
        };
    }

    private String getStatusLabel(Integer status) {

        if (status == null) {
            return null;
        }

        return switch (status) {
            case 1 -> "Hoạt động";
            case 0 -> "Ngừng hoạt động";
            default -> "Không xác định";
        };
    }

    // =========================================================
    // ADDRESS
    // =========================================================
    private String buildAddress(NhanVien nv) {

        StringBuilder address = new StringBuilder();

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

        if (value == null || value.isBlank()) {
            return;
        }

        if (address.length() > 0) {
            address.append(", ");
        }

        address.append(value.trim());
    }

    // =========================================================
    // NORMALIZE
    // =========================================================
    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String result = value.trim();

        return result.isEmpty()
                ? null
                : result;
    }

    private String normalizeImage(String image) {

        String result = normalizeNullable(image);

        if (result == null) {
            return null;
        }

        /*
         * EmployeeForm hiện đọc ảnh thành base64.
         * Không lưu base64 vào hinh_anh NVARCHAR(1000).
         */
        if (result.startsWith("data:image/")) {
            throw AppException.badRequest("Hãy tải ảnh đại diện lên trước khi lưu nhân viên");
        }

        if (result.length() > 1000) {
            throw AppException.badRequest(
                    "Đường dẫn hình ảnh quá dài"
            );
        }

        return result;
    }
}
