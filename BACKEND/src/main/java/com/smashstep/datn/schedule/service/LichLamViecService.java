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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LichLamViecService {

    private final LichLamViecRepository lichLamViecRepository;
    private final CaLamViecRepository   caLamViecRepository;
    private final NhanVienRepository    nhanVienRepository;
    private final com.smashstep.datn.product.service.KhoaGhiSanPham khoaGhi;
    private final LichLamViecImportService nhapExcel;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("HH:mm\ndd/MM/yyyy");

    // ── Danh sách mã NV đã có lịch trong ca+ngày ─────────
    public List<String> getMaNhanVienTrongCa(Long caId, LocalDate ngayLam) {
        return lichLamViecRepository.findMaNhanVienTrongCa(caId, ngayLam);
    }

    // ── Lịch cần xếp lại (nhân viên bị khóa) ─────────────
    /**
     * Trả về các lịch tương lai còn mở (trangThai 0 hoặc 1) của nhân viên bị khóa.
     * Admin dùng để biết cần xếp lại ca cho ai.
     */
    public List<LichLamViecResponse> getLichCuaNhanVienBiKhoa() {
        return lichLamViecRepository
                .findLichCuaNhanVienBiKhoa(LocalDate.now())
                .stream()
                .map(l -> toResponse(l, false))
                .toList();
    }

    // ── Ca hôm nay của nhân viên ──────────────────────────
    public List<LichLamViecResponse> getCaHomNay(Long nhanVienId) {
        LocalDate today = LocalDate.now();
        return lichLamViecRepository.findForEmployee(nhanVienId,today,today).stream()
                .map(l -> toResponse(l, false))
                .toList();
    }

    // ── Danh sách thường ───────────────────────────────────
    public PageResponse<LichLamViecResponse> getAll(
            LocalDate tuNgay, LocalDate denNgay,
            Long caId, String tuKhoa,
            int page, int size) {
        return search(tuNgay, denNgay, caId, null, tuKhoa, page, size, false);
    }

    // ── Lịch sử hoạt động (có vai trò + doanh thu) ────────
    public PageResponse<LichLamViecResponse> getLichSuHoatDong(
            LocalDate tuNgay, LocalDate denNgay,
            Long caId, Long vaiTroId, String tuKhoa,
            int page, int size) {
        return search(tuNgay, denNgay, caId, vaiTroId, tuKhoa, page, size, true);
    }

    private PageResponse<LichLamViecResponse> search(
            LocalDate tuNgay, LocalDate denNgay,
            Long caId, Long vaiTroId, String tuKhoa,
            int page, int size, boolean tinhDoanhThu) {

        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) throw AppException.badRequest("Khoảng ngày không hợp lệ");
        if (page < 1) page = 1;
        size = Math.min(Math.max(size, 1), 100);

        var pageable = PageRequest.of(page - 1, size);
        var result   = lichLamViecRepository.search(tuNgay, denNgay, caId, vaiTroId, tuKhoa, pageable);
        return PageResponse.from(result.map(l -> toResponse(l, tinhDoanhThu)));
    }

    public List<LichLamViecResponse> getByWeek(LocalDate tuNgay) {
        LocalDate denNgay = tuNgay.plusDays(6);
        return lichLamViecRepository.findByNgayLamBetween(tuNgay, denNgay)
                .stream().map(l -> toResponse(l, false)).toList();
    }

    // ── Tạo mới ────────────────────────────────────────────
    @Transactional
    public List<LichLamViecResponse> create(LichLamViecRequest request) {
        khoaGhi.khoa("schedule");
        if(request==null||request.getNgayLam()==null||request.getMaNhanViens()==null||request.getMaNhanViens().isEmpty()||request.getMaNhanViens().size()>100)
            throw AppException.badRequest("Cần ngày làm và từ 1 đến 100 nhân viên");
        if(request.getGhiChu()!=null&&request.getGhiChu().length()>500)throw AppException.badRequest("Ghi chú tối đa 500 ký tự");
        CaLamViec ca=caLamViecRepository.findById(request.getCaLamViecId()).orElseThrow(()->AppException.notFound("Không tìm thấy ca làm việc"));
        if(!Integer.valueOf(1).equals(ca.getTrangThai()))throw AppException.badRequest("Ca làm việc đã ngừng hoạt động");
        QuyTacLichLamViec.khoang(request.getNgayLam(),ca);
        var seen=new java.util.HashSet<Long>();var staff=new ArrayList<NhanVien>();
        for(String code:request.getMaNhanViens()){
            if(code==null||code.isBlank()||code.length()>50)throw AppException.badRequest("Mã nhân viên không hợp lệ");
            var nv=nhanVienRepository.findByMaNhanVien(code.trim()).orElseThrow(()->AppException.notFound("Không tìm thấy nhân viên: "+code));
            if(!Integer.valueOf(1).equals(nv.getTrangThai()))throw AppException.badRequest("Nhân viên "+code+" đang bị khóa");
            if(!seen.add(nv.getId()))throw AppException.badRequest("Nhân viên bị lặp trong yêu cầu phân ca");
            if(chongLich(nv.getId(),request.getNgayLam(),ca))throw AppException.conflict("Nhân viên "+code+" đã có lịch trùng ca hoặc chồng giờ (kể cả ca qua đêm)");
            staff.add(nv);
        }
        var results=new ArrayList<LichLamViecResponse>();var now=LocalDateTime.now();
        for(var nv:staff){
            var row=new LichLamViec();row.setIdNhanVien(nv);row.setIdCaLamViec(ca);row.setNgayLam(request.getNgayLam());row.setTrangThai(0);
            row.setGhiChu(request.getGhiChu());row.setNgayTao(now);row.setNgayCapNhat(now);
            results.add(toResponse(lichLamViecRepository.save(row),false));
        }
        return results;
    }
    private boolean chongLich(Long employee,LocalDate date,CaLamViec ca){
        return lichLamViecRepository.findForEmployee(employee,date.minusDays(1),date.plusDays(1)).stream()
                .anyMatch(row->QuyTacLichLamViec.chong(date,ca,row));
    }
    @Transactional
    public LichLamViecResponse thayNhanVien(Long id,String code){
        khoaGhi.khoa("schedule");
        var old=lichLamViecRepository.findById(id).orElseThrow(()->AppException.notFound("Không tìm thấy lịch cần thay"));
        if(old.getTrangThai()==null||old.getTrangThai()>1)throw AppException.conflict("Chỉ thay nhân viên của lịch chưa kết thúc/chưa đánh dấu vắng mặt");
        if(code==null||code.isBlank()||code.length()>50)throw AppException.badRequest("Cần mã nhân viên thay thế hợp lệ");
        var req=new LichLamViecRequest();req.setMaNhanViens(List.of(code));req.setCaLamViecId(old.getIdCaLamViec().getId());req.setNgayLam(old.getNgayLam());
        req.setGhiChu("Thay thế "+old.getIdNhanVien().getMaNhanVien());
        var result=create(req).get(0);lichLamViecRepository.delete(old);lichLamViecRepository.flush();return result;
    }

    // ── Cập nhật trạng thái ────────────────────────────────
    @Transactional
    public LichLamViecResponse updateTrangThai(Long id, CapNhatTrangThaiRequest request) {
        khoaGhi.khoa("schedule");
        if(request==null||request.getTrangThai()==null)throw AppException.badRequest("Cần trạng thái hợp lệ");
        if(request.getGhiChu()!=null&&request.getGhiChu().length()>500)throw AppException.badRequest("Ghi chú tối đa 500 ký tự");
        LichLamViec llv = lichLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch làm việc"));

        if (request.getTrangThai() < 0 || request.getTrangThai() > 3) {
            throw AppException.badRequest("Trạng thái không hợp lệ (0-3)");
        }

        llv.setTrangThai(request.getTrangThai());
        if (request.getGhiChu() != null) llv.setGhiChu(request.getGhiChu());
        llv.setNgayCapNhat(LocalDateTime.now());

        return toResponse(lichLamViecRepository.save(llv), false);
    }

    // ── Xóa ───────────────────────────────────────────────
    @Transactional
    public void delete(Long id) {
        khoaGhi.khoa("schedule");
        LichLamViec llv = lichLamViecRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch làm việc"));
        lichLamViecRepository.delete(llv);
    }

    // ── Xếp ca tự động ─────────────────────────────────────
    /**
     * Phân ca tự động cho toàn bộ nhân viên đang hoạt động trong khoảng tuần [tuNgay, tuNgay+6].
     * Mỗi nhân viên được ghép luân phiên với từng ca đang hoạt động theo từng ngày.
     * Bỏ qua nếu nhân viên đã có lịch trong ca+ngày đó.
     */
    @Transactional
    public Map<String, Object> xepCaTuDong(LocalDate tuNgay, LocalDate denNgay) {
        khoaGhi.khoa("schedule");QuyTacLichLamViec.khoangNgay(tuNgay,denNgay);
        List<NhanVien> nhanViens = nhanVienRepository.findAll().stream()
                .filter(nv -> Integer.valueOf(1).equals(nv.getTrangThai()))
                .toList();

        List<CaLamViec> caLamViecs = caLamViecRepository.findAll().stream()
                .filter(ca -> Integer.valueOf(1).equals(ca.getTrangThai()))
                .toList();

        if (caLamViecs.isEmpty()) throw AppException.badRequest("Không có ca làm việc nào đang hoạt động");
        if (nhanViens.isEmpty())  throw AppException.badRequest("Không có nhân viên nào đang hoạt động");

        int created = 0, skipped = 0;
        LocalDate ngay = tuNgay;
        int caIdx = 0;

        while (!ngay.isAfter(denNgay)) {
            for (NhanVien nv : nhanViens) {
                CaLamViec ca = caLamViecs.get(caIdx % caLamViecs.size());
                caIdx++;

                if (chongLich(nv.getId(), ngay, ca)) {
                    skipped++;
                    continue;
                }

                LichLamViec llv = new LichLamViec();
                llv.setIdNhanVien(nv);
                llv.setIdCaLamViec(ca);
                llv.setNgayLam(ngay);
                llv.setTrangThai(0);
                llv.setNgayTao(LocalDateTime.now());
                llv.setNgayCapNhat(LocalDateTime.now());
                lichLamViecRepository.save(llv);
                created++;
            }
            ngay = ngay.plusDays(1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("daTao", created);
        result.put("boBua", skipped);
        result.put("boQua", skipped);
        result.put("thongBao", "Xếp ca tự động hoàn tất: tạo " + created + " lịch, bỏ qua " + skipped + " (đã tồn tại hoặc chồng giờ).");
        return result;
    }

    // ── Tải template Excel ─────────────────────────────────
    public byte[] downloadTemplate() throws IOException {
        List<NhanVien> nhanViens = nhanVienRepository.findAll().stream()
                .filter(nv -> Integer.valueOf(1).equals(nv.getTrangThai()))
                .sorted(java.util.Comparator.comparing(NhanVien::getMaNhanVien))
                .toList();
        List<CaLamViec> cas = caLamViecRepository.findAll().stream()
                .filter(ca -> Integer.valueOf(1).equals(ca.getTrangThai()))
                .sorted(java.util.Comparator.comparing(CaLamViec::getMaCa))
                .toList();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── Sheet ẩn: _NhanVien (cột A=maNV, cột B=tenNV) ──
            Sheet nvSheet = wb.createSheet("_NhanVien");
            wb.setSheetHidden(wb.getSheetIndex("_NhanVien"), true);
            for (int i = 0; i < nhanViens.size(); i++) {
                Row r = nvSheet.createRow(i);
                r.createCell(0).setCellValue(nhanViens.get(i).getMaNhanVien());
                r.createCell(1).setCellValue(nhanViens.get(i).getTenNhanVien());
            }

            // ── Sheet ẩn: _CaLamViec (cột A=maCa, cột B=tenCa) ──
            Sheet caSheet = wb.createSheet("_CaLamViec");
            wb.setSheetHidden(wb.getSheetIndex("_CaLamViec"), true);
            for (int i = 0; i < cas.size(); i++) {
                Row r = caSheet.createRow(i);
                r.createCell(0).setCellValue(cas.get(i).getMaCa());
                r.createCell(1).setCellValue(cas.get(i).getTenCa()
                        + " (" + fmt(cas.get(i).getGioBatDau())
                        + "-" + fmt(cas.get(i).getGioKetThuc()) + ")");
            }

            // ── Sheet chính: LichLamViec ──
            Sheet sheet = wb.createSheet("LichLamViec");

            // Styles
            CellStyle hStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setBorderBottom(BorderStyle.THIN);
            hStyle.setBorderTop(BorderStyle.THIN);
            hStyle.setBorderLeft(BorderStyle.THIN);
            hStyle.setBorderRight(BorderStyle.THIN);
            hStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            CellStyle dStyle = wb.createCellStyle();
            dStyle.setBorderBottom(BorderStyle.THIN);
            dStyle.setBorderTop(BorderStyle.THIN);
            dStyle.setBorderLeft(BorderStyle.THIN);
            dStyle.setBorderRight(BorderStyle.THIN);

            // Style readonly (màu xám nhạt — dùng cho cột tự động)
            CellStyle roStyle = wb.createCellStyle();
            roStyle.cloneStyleFrom(dStyle);
            roStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            roStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font roFont = wb.createFont();
            roFont.setItalic(true);
            roFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            roStyle.setFont(roFont);

            // Header row
            // Cột: A=maNhanVien, B=tenNhanVien(auto), C=maCa, D=tenCa(auto), E=ngayLam, F=ghiChu
            String[] headers = {
                    "maNhanVien (*)",
                    "Tên nhân viên (tự động)",
                    "maCa (*)",
                    "Tên ca (tự động)",
                    "ngayLam yyyy-MM-dd (*)",
                    "ghiChu"
            };
            int[] colWidths = {5000, 8000, 4000, 8000, 6000, 6000};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(hStyle);
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // Số dòng data tối đa hỗ trợ
            int maxRows = 500;
            int nvCount  = nhanViens.size();
            int caCount  = cas.size();

            // Chuẩn bị dòng trống và VLOOKUP cho tối đa maxRows dòng
            // No prefilled employee, shift or date in the blank template.
            for (int r = 1; r <= maxRows; r++) {
                Row row = sheet.createRow(r);

                // Cột A — maNhanVien: người dùng chọn từ danh sách
                Cell cellA = row.createCell(0);
                cellA.setCellStyle(dStyle);

                // Cột B — tenNhanVien: VLOOKUP tự động
                Cell cellB = row.createCell(1);
                if (nvCount > 0) {
                    cellB.setCellFormula(
                            "IF(A" + (r+1) + "=\"\",\"\","
                                    + "IFERROR(VLOOKUP(A" + (r+1) + ",_NhanVien!$A:$B,2,0),\"Không tìm thấy\"))"
                    );
                }
                cellB.setCellStyle(roStyle);

                // Cột C — maCa: người dùng chọn từ danh sách
                Cell cellC = row.createCell(2);
                cellC.setCellStyle(dStyle);

                // Cột D — tenCa: VLOOKUP tự động
                Cell cellD = row.createCell(3);
                if (caCount > 0) {
                    cellD.setCellFormula(
                            "IF(C" + (r+1) + "=\"\",\"\","
                                    + "IFERROR(VLOOKUP(C" + (r+1) + ",_CaLamViec!$A:$B,2,0),\"Không tìm thấy\"))"
                    );
                }
                cellD.setCellStyle(roStyle);

                // Cột E — ngayLam: người dùng nhập ngày làm
                Cell cellE = row.createCell(4);
                // Leave the date empty; no example assignment is saved accidentally.
                cellE.setCellStyle(dStyle);

                // Cột F — ghiChu
                Cell cellF = row.createCell(5);
                cellF.setCellStyle(dStyle);
            }

            // Data validation — dropdown maNhanVien (cột A)
            if (nvCount > 0) {
                org.apache.poi.ss.util.CellRangeAddressList nvRange =
                        new org.apache.poi.ss.util.CellRangeAddressList(1, maxRows, 0, 0);
                org.apache.poi.xssf.usermodel.XSSFDataValidationHelper dvHelper =
                        new org.apache.poi.xssf.usermodel.XSSFDataValidationHelper((org.apache.poi.xssf.usermodel.XSSFSheet) sheet);
                org.apache.poi.ss.usermodel.DataValidationConstraint nvConstraint =
                        dvHelper.createFormulaListConstraint("_NhanVien!$A$1:$A$" + nvCount);
                org.apache.poi.ss.usermodel.DataValidation nvDv = dvHelper.createValidation(nvConstraint, nvRange);
                nvDv.setShowErrorBox(true);
                nvDv.createErrorBox("Lỗi", "Mã nhân viên không hợp lệ. Vui lòng chọn từ danh sách.");
                sheet.addValidationData(nvDv);
            }

            // Data validation — dropdown maCa (cột C)
            if (caCount > 0) {
                org.apache.poi.ss.util.CellRangeAddressList caRange =
                        new org.apache.poi.ss.util.CellRangeAddressList(1, maxRows, 2, 2);
                org.apache.poi.xssf.usermodel.XSSFDataValidationHelper dvHelper =
                        new org.apache.poi.xssf.usermodel.XSSFDataValidationHelper((org.apache.poi.xssf.usermodel.XSSFSheet) sheet);
                org.apache.poi.ss.usermodel.DataValidationConstraint caConstraint =
                        dvHelper.createFormulaListConstraint("_CaLamViec!$A$1:$A$" + caCount);
                org.apache.poi.ss.usermodel.DataValidation caDv = dvHelper.createValidation(caConstraint, caRange);
                caDv.setShowErrorBox(true);
                caDv.createErrorBox("Lỗi", "Mã ca không hợp lệ. Vui lòng chọn từ danh sách.");
                sheet.addValidationData(caDv);
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            // ── Sheet hướng dẫn ──
            Sheet guide = wb.createSheet("HuongDan");
            String[] lines = {
                    "HƯỚNG DẪN IMPORT LỊCH LÀM VIỆC",
                    "",
                    "Sheet 'LichLamViec' gồm 6 cột:",
                    "  - maNhanVien (*)          : Mã nhân viên — có dropdown gợi ý, bắt buộc",
                    "  - Tên nhân viên (tự động) : Tự động hiện khi nhập đúng mã (không cần nhập)",
                    "  - maCa (*)                : Mã ca làm việc — có dropdown gợi ý, bắt buộc",
                    "  - Tên ca (tự động)        : Tự động hiện khi nhập đúng mã (không cần nhập)",
                    "  - ngayLam (*)             : Ngày làm, định dạng yyyy-MM-dd, ví dụ: 2026-10-16, bắt buộc",
                    "  - ghiChu                  : Ghi chú (tuỳ chọn)",
                    "",
                    "Lưu ý:",
                    "  - Các cột (*) là bắt buộc.",
                    "  - Cột 'Tên nhân viên' và 'Tên ca' chỉ để tham khảo, không cần nhập.",
                    "  - Nếu tên hiện 'Không tìm thấy' nghĩa là mã không đúng.",
                    "  - Lịch đã có trong DB được bỏ qua; trùng trong file hoặc chồng giờ sẽ báo lỗi.",
                    "  - Xem trước và sửa hết lỗi rồi mới xác nhận. Có lỗi thì chưa lưu bất kỳ dòng nào.",
                    "  - Ghi chú tối đa 500 ký tự. Bỏ qua các dòng chưa chọn nhân viên/ca và chưa nhập ghi chú.",
                    "  - Hỗ trợ tối đa 500 dòng mỗi lần import."
            };
            CellStyle guideStyle = wb.createCellStyle();
            Font guideFont = wb.createFont();
            guideStyle.setFont(guideFont);
            for (int i = 0; i < lines.length; i++) {
                Row r = guide.createRow(i);
                Cell c = r.createCell(0);
                c.setCellValue(lines[i]);
                if (i == 0) {
                    Font boldFont = wb.createFont();
                    boldFont.setBold(true);
                    boldFont.setFontHeightInPoints((short) 13);
                    CellStyle titleStyle = wb.createCellStyle();
                    titleStyle.setFont(boldFont);
                    c.setCellStyle(titleStyle);
                }
            }
            guide.setColumnWidth(0, 22000);

            // Đặt LichLamViec là sheet active
            wb.setActiveSheet(wb.getSheetIndex("LichLamViec"));

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Import Excel ───────────────────────────────────────
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file) throws IOException {
        var checked=nhapExcel.xemTruoc(file);
        if(checked.coLoi()>0)throw new com.smashstep.datn.schedule.exception.LichLamViecImportException(checked);
        var result=nhapExcel.xacNhan(new com.smashstep.datn.schedule.dto.LichLamViecImportDto.XacNhan(checked.dong().stream().map(r->r.duLieu()).toList()));
        return Map.of("daTao",result.daTao(),"boQua",result.boQua(),"boBua",result.boQua(),"loi",0,"loiio",0,"chiTietLoi",List.of(),"thongBao",result.thongBao());
    }

    // ── Xuất Excel lịch sử hoạt động ──────────────────────────
    public byte[] xuatExcelLichSu(
            LocalDate tuNgay, LocalDate denNgay,
            Long caId, Long vaiTroId, String tuKhoa) throws IOException {

        // Lấy toàn bộ dữ liệu (tối đa 5000 dòng)
        var pageable = PageRequest.of(0, 5000);
        var result   = lichLamViecRepository.search(tuNgay, denNgay, caId, vaiTroId, tuKhoa, pageable);
        List<LichLamViecResponse> data = result.stream()
                .map(l -> toResponse(l, true)).toList();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("LichSuHoatDong");

            // ── Styles ──
            // Title style
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Header style
            CellStyle hStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            hStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            setBorder(hStyle);

            // Data style
            CellStyle dStyle = wb.createCellStyle();
            dStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            setBorder(dStyle);

            // Data center style
            CellStyle dcStyle = wb.createCellStyle();
            dcStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            dcStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            setBorder(dcStyle);

            // Money style
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
            moneyStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            setBorder(moneyStyle);
            org.apache.poi.ss.usermodel.DataFormat fmt = wb.createDataFormat();
            moneyStyle.setDataFormat(fmt.getFormat("#,##0"));

            // Stripe style (row chẵn)
            CellStyle stripeStyle = wb.createCellStyle();
            stripeStyle.cloneStyleFrom(dStyle);
            stripeStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            stripeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle stripeDcStyle = wb.createCellStyle();
            stripeDcStyle.cloneStyleFrom(dcStyle);
            stripeDcStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            stripeDcStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle stripeMoneyStyle = wb.createCellStyle();
            stripeMoneyStyle.cloneStyleFrom(moneyStyle);
            stripeMoneyStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            stripeMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            stripeMoneyStyle.setDataFormat(fmt.getFormat("#,##0"));

            // ── Tiêu đề file ──
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            String dateRange = (tuNgay != null ? tuNgay.format(DATE_FMT) : "?")
                    + " - " + (denNgay != null ? denNgay.format(DATE_FMT) : "?");
            titleCell.setCellValue("LỊCH SỬ HOẠT ĐỘNG  |  " + dateRange);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            // ── Header row ──
            String[] headers = {
                    "STT", "Mã NV", "Tên nhân viên", "Vai trò",
                    "Ca làm việc", "Ngày làm", "Giờ bắt đầu", "Giờ kết thúc",
                    "Doanh thu (đ)", "Trạng thái", "Ghi chú"
            };
            int[] colWidths = { 1500, 3500, 7000, 4000, 5000, 4000, 4500, 4500, 5500, 4500, 7000 };

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // ── Data rows ──
            int rowIdx = 2;
            for (int i = 0; i < data.size(); i++) {
                LichLamViecResponse r = data.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);
                boolean stripe = i % 2 == 1;

                CellStyle cs  = stripe ? stripeStyle     : dStyle;
                CellStyle csc = stripe ? stripeDcStyle   : dcStyle;
                CellStyle csm = stripe ? stripeMoneyStyle: moneyStyle;

                setCell(row, 0, i + 1,                         csc);
                setCell(row, 1, r.getMaNhanVien(),              cs);
                setCell(row, 2, r.getTenNhanVien(),             cs);
                setCell(row, 3, r.getVaiTro(),                  cs);
                setCell(row, 4, r.getTenCa() + " (" + r.getThoiGian() + ")", cs);
                setCell(row, 5, r.getNgayLam(),                 csc);
                setCell(row, 6, r.getThoiGianBatDau() != null
                        ? r.getThoiGianBatDau().replace("\n", " ") : "", csc);
                setCell(row, 7, r.getThoiGianKetThuc() != null
                        ? r.getThoiGianKetThuc().replace("\n", " ") : "", csc);

                // Doanh thu — số
                Cell moneyCell = row.createCell(8);
                moneyCell.setCellValue(r.getDoanhThu() != null
                        ? r.getDoanhThu().doubleValue() : 0);
                moneyCell.setCellStyle(csm);

                setCell(row, 9,  r.getTrangThaiLabel(),         csc);
                setCell(row, 10, r.getGhiChu() != null ? r.getGhiChu() : "", cs);
            }

            // ── Dòng tổng ──
            if (!data.isEmpty()) {
                Row totalRow = sheet.createRow(rowIdx);
                totalRow.setHeightInPoints(20);

                CellStyle totalStyle = wb.createCellStyle();
                Font totalFont = wb.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                totalStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT);
                totalStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
                setBorder(totalStyle);
                totalStyle.setDataFormat(fmt.getFormat("#,##0"));

                CellStyle totalLabelStyle = wb.createCellStyle();
                totalLabelStyle.cloneStyleFrom(totalStyle);
                totalLabelStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);

                Cell labelCell = totalRow.createCell(0);
                labelCell.setCellValue("TỔNG CỘNG: " + data.size() + " bản ghi");
                labelCell.setCellStyle(totalLabelStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx, rowIdx, 0, 7));

                // Tổng doanh thu
                double tongDT = data.stream()
                        .mapToDouble(r -> r.getDoanhThu() != null ? r.getDoanhThu().doubleValue() : 0)
                        .sum();
                Cell sumCell = totalRow.createCell(8);
                sumCell.setCellValue(tongDT);
                sumCell.setCellStyle(totalStyle);

                for (int i = 9; i <= 10; i++) {
                    Cell ec = totalRow.createCell(i);
                    ec.setCellStyle(totalLabelStyle);
                }
            }

            // Freeze header
            sheet.createFreezePane(0, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void setCell(Row row, int col, Object val, CellStyle style) {
        Cell cell = row.createCell(col);
        if (val instanceof Integer v) cell.setCellValue(v);
        else if (val instanceof String s) cell.setCellValue(s);
        else cell.setCellValue(val != null ? val.toString() : "");
        cell.setCellStyle(style);
    }
    /**
     * Chạy mỗi phút. Tìm tất cả lịch có trangThai = 0 (Chờ giao ca) hoặc 1 (Giao ca)
     * mà thời gian kết thúc ca đã qua → tự động chuyển sang 3 (Đã kết thúc).
     */
    @Transactional
    public int tuDongKetThuc() {
        khoaGhi.khoa("schedule");
        LocalDateTime now = LocalDateTime.now();
        List<LichLamViec> expired = lichLamViecRepository.findExpiredLich(now);
        for (LichLamViec llv : expired) {
            llv.setTrangThai(3);
            llv.setNgayCapNhat(now);
            lichLamViecRepository.save(llv);
        }
        return expired.size();
    }

    private String cellStr(Cell cell) {        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            default -> "";
        };
    }

    // ── Mapping ────────────────────────────────────────────
    private LichLamViecResponse toResponse(LichLamViec llv, boolean tinhDoanhThu) {
        NhanVien nv  = llv.getIdNhanVien();
        CaLamViec ca = llv.getIdCaLamViec();

        String thoiGian = fmt(ca.getGioBatDau()) + " - " + fmt(ca.getGioKetThuc());

        // Thời gian bắt đầu / kết thúc dạng "HH:mm\ndd/MM/yyyy"
        String tgBatDau  = null;
        String tgKetThuc = null;
        if (llv.getNgayLam() != null && ca.getGioBatDau() != null) {
            LocalDate ngay = llv.getNgayLam();
            tgBatDau = fmt(ca.getGioBatDau()) + "\n" + ngay.format(DATE_FMT);

            // Ca qua đêm: gioKetThuc < gioBatDau → kết thúc hôm sau
            LocalDate ngayKT = ca.getGioKetThuc() != null
                    && ca.getGioKetThuc().isBefore(ca.getGioBatDau())
                    ? ngay.plusDays(1) : ngay;
            if (ca.getGioKetThuc() != null) {
                tgKetThuc = fmt(ca.getGioKetThuc()) + "\n" + ngayKT.format(DATE_FMT);
            }
        }

        // Doanh thu
        BigDecimal doanhThu = BigDecimal.ZERO;
        if (tinhDoanhThu && llv.getNgayLam() != null
                && ca.getGioBatDau() != null && ca.getGioKetThuc() != null) {
            LocalDate ngay    = llv.getNgayLam();
            LocalTime gioBD   = ca.getGioBatDau();
            LocalTime gioKT   = ca.getGioKetThuc();
            LocalDateTime batDau  = ngay.atTime(gioBD);
            LocalDate ngayKT  = gioKT.isBefore(gioBD) ? ngay.plusDays(1) : ngay;
            LocalDateTime ketThuc = ngayKT.atTime(gioKT);
            doanhThu = lichLamViecRepository.tinhDoanhThu(nv.getId(), batDau, ketThuc);
            if (doanhThu == null) doanhThu = BigDecimal.ZERO;
        }

        String vaiTro = nv.getIdVaiTro() != null ? nv.getIdVaiTro().getTenVaiTro() : "—";

        return LichLamViecResponse.builder()
                .id(llv.getId())
                .maNhanVien(nv.getMaNhanVien())
                .tenNhanVien(nv.getTenNhanVien())
                .hinhAnh(nv.getHinhAnh())
                .vaiTro(vaiTro)
                .caLamViecId(ca.getId())
                .maCa(ca.getMaCa())
                .tenCa(ca.getTenCa())
                .thoiGian(thoiGian)
                .ngayLam(llv.getNgayLam() != null ? llv.getNgayLam().toString() : null)
                .thoiGianBatDau(tgBatDau)
                .thoiGianKetThuc(tgKetThuc)
                .doanhThu(doanhThu)
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
            case 0 -> "Chờ giao ca";
            case 1 -> "Giao ca";
            case 2 -> "Vắng mặt";
            case 3 -> "Đã kết thúc";
            default -> "Không xác định";
        };
    }
}
