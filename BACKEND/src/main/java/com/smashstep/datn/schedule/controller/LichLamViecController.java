package com.smashstep.datn.schedule.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.employee.entity.VaiTro;
import com.smashstep.datn.employee.repository.VaiTroRepository;
import com.smashstep.datn.schedule.dto.CapNhatTrangThaiRequest;
import com.smashstep.datn.schedule.dto.LichLamViecRequest;
import com.smashstep.datn.schedule.dto.LichLamViecResponse;
import com.smashstep.datn.schedule.service.LichLamViecService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lich-lam-viec")
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService lichLamViecService;
    private final VaiTroRepository   vaiTroRepository;

    /** Danh sách vai trò (cho bộ lọc lịch sử) */
    @GetMapping("/vai-tro")
    public ApiResponse<List<VaiTro>> getVaiTro() {
        return ApiResponse.ok("Lấy vai trò thành công",
                vaiTroRepository.findByTrangThaiOrderByIdAsc(1));
    }

    /**
     * Danh sách mã nhân viên đã có lịch trong ca + ngày chỉ định.
     * Dùng để disable NV đã trùng ca khi thay thế.
     */
    @GetMapping("/nhan-vien-trong-ca")
    public ApiResponse<List<String>> getNhanVienTrongCa(
            @RequestParam Long caId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayLam) {
        return ApiResponse.ok("OK", lichLamViecService.getMaNhanVienTrongCa(caId, ngayLam));
    }

    /**
     * Lịch tương lai cần xếp lại — nhân viên bị khóa còn có lịch mở.
     * Chỉ ADMIN mới xem được.
     */
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nhan-vien-bi-khoa")
    public ApiResponse<List<LichLamViecResponse>> getLichNhanVienBiKhoa() {
        return ApiResponse.ok("Lấy lịch nhân viên bị khóa thành công",
                lichLamViecService.getLichCuaNhanVienBiKhoa());
    }

    /** Ca làm việc hôm nay của nhân viên đang đăng nhập */
    @GetMapping("/ca-hom-nay")
    public ApiResponse<List<LichLamViecResponse>> getCaHomNay(
            org.springframework.security.core.Authentication authentication
    ) {
        com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails details =
                (com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails) authentication.getDetails();
        return ApiResponse.ok("Ca hôm nay",
                lichLamViecService.getCaHomNay(details.userId()));
    }

    /**
     * Kiểm tra ca sắp bắt đầu trong 15 phút (trangThai 0 hoặc 4).
     * Dùng cho popup giao ca tự động ở frontend.
     * Trả về null trong data nếu không có ca nào cần xác nhận.
     */
    @GetMapping("/ca-sap-bat-dau")
    public ApiResponse<com.smashstep.datn.schedule.dto.GiaoCaResponse> getCaSapBatDau(
            org.springframework.security.core.Authentication authentication
    ) {
        com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails details =
                (com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails) authentication.getDetails();
        return ApiResponse.ok("Ca sắp bắt đầu",
                lichLamViecService.getCaSapBatDau(details.userId()));
    }

    /** Xác nhận nhận ca — chuyển trangThai → 1 (Đang làm) */
    @PostMapping("/{id}/xac-nhan-nhan-ca")
    public ApiResponse<LichLamViecResponse> xacNhanNhanCa(@PathVariable Long id) {
        return ApiResponse.ok("Xác nhận nhận ca thành công",
                lichLamViecService.xacNhanNhanCa(id));
    }

    /** Từ chối nhận ca — chuyển trangThai → 4 (Chưa nhận ca) */
    @PostMapping("/{id}/chua-nhan-ca")
    public ApiResponse<LichLamViecResponse> chuaNhanCa(@PathVariable Long id) {
        return ApiResponse.ok("Đã đánh dấu chưa nhận ca",
                lichLamViecService.chuaNhanCa(id));
    }

    /**
     * Kiểm tra ca đang làm của nhân viên (trangThai = 1).
     * Dùng để hiển thị nút "Kết thúc ca" ở frontend.
     * Trả về null trong data nếu không có ca đang làm.
     */
    @GetMapping("/ca-dang-lam")
    public ApiResponse<com.smashstep.datn.schedule.dto.KetThucCaResponse> getCaDangLam(
            org.springframework.security.core.Authentication authentication
    ) {
        com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails details =
                (com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails) authentication.getDetails();
        return ApiResponse.ok("Ca đang làm",
                lichLamViecService.getCaDangLam(details.userId()));
    }

    /** Kết thúc ca — chuyển trangThai → 3 (Đã kết thúc) */
    @PostMapping("/{id}/ket-thuc-ca")
    public ApiResponse<LichLamViecResponse> ketThucCa(
            @PathVariable Long id,
            @Valid @RequestBody com.smashstep.datn.schedule.dto.KetThucCaRequest request
    ) {
        return ApiResponse.ok("Kết thúc ca thành công",
                lichLamViecService.ketThucCa(id, request));
    }

    /** Danh sách có phân trang + filter */
    @GetMapping
    public ApiResponse<PageResponse<LichLamViecResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long caId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok("Lấy lịch làm việc thành công",
                lichLamViecService.getAll(tuNgay, denNgay, caId, tuKhoa, page, size));
    }

    /** Lịch sử hoạt động — có vai trò + doanh thu */
    @GetMapping("/lich-su-hoat-dong")
    public ApiResponse<PageResponse<LichLamViecResponse>> getLichSuHoatDong(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long caId,
            @RequestParam(required = false) Long vaiTroId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok("Lấy lịch sử hoạt động thành công",
                lichLamViecService.getLichSuHoatDong(tuNgay, denNgay, caId, vaiTroId, tuKhoa, page, size));
    }

    /** Lấy toàn bộ lịch trong 1 tuần (cho calendar view) */
    @GetMapping("/tuan")
    public ApiResponse<List<LichLamViecResponse>> getByWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay
    ) {
        return ApiResponse.ok("Lấy lịch tuần thành công", lichLamViecService.getByWeek(tuNgay));
    }

    /** Phân ca — có thể phân cho nhiều nhân viên cùng lúc */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<LichLamViecResponse>> create(
            @Valid @RequestBody LichLamViecRequest request
    ) {
        return ApiResponse.ok("Phân ca thành công", lichLamViecService.create(request));
    }

    /** Cập nhật trạng thái (xác nhận / vắng / hoàn thành) */
    @PatchMapping("/{id}/trang-thai")
    public ApiResponse<LichLamViecResponse> updateTrangThai(
            @PathVariable Long id,
            @Valid @RequestBody CapNhatTrangThaiRequest request
    ) {
        return ApiResponse.ok("Cập nhật trạng thái thành công",
                lichLamViecService.updateTrangThai(id, request));
    }

    /** Xóa lịch */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lichLamViecService.delete(id);
        return ApiResponse.<Void>ok("Xóa lịch làm việc thành công");
    }

    /** Xếp ca tự động cho khoảng ngày */
    @PostMapping("/xep-ca-tu-dong")
    public ApiResponse<Map<String, Object>> xepCaTuDong(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay
    ) {
        if (denNgay.isBefore(tuNgay)) {
            throw com.smashstep.datn.common.exception.AppException.badRequest("denNgay không được trước tuNgay");
        }
        if (tuNgay.plusDays(30).isBefore(denNgay)) {
            throw com.smashstep.datn.common.exception.AppException.badRequest("Khoảng thời gian tối đa 31 ngày");
        }
        return ApiResponse.ok("Xếp ca tự động thành công", lichLamViecService.xepCaTuDong(tuNgay, denNgay));
    }

    /** Tải file template Excel */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = lichLamViecService.downloadTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("template-lich-lam-viec.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    /** Xuất Excel lịch sử hoạt động */
    @GetMapping("/xuat-excel-lich-su")
    public ResponseEntity<byte[]> xuatExcelLichSu(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Long caId,
            @RequestParam(required = false) Long vaiTroId,
            @RequestParam(required = false) String tuKhoa
    ) throws IOException {
        byte[] data = lichLamViecService.xuatExcelLichSu(tuNgay, denNgay, caId, vaiTroId, tuKhoa);
        String filename = "lich-su-hoat-dong"
                + (tuNgay != null ? "-" + tuNgay : "")
                + (denNgay != null ? "-" + denNgay : "")
                + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }    /** Import lịch làm việc từ file Excel */
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ApiResponse.ok("Import hoàn tất", lichLamViecService.importExcel(file));
    }
}
