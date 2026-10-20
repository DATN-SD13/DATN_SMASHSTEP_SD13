package com.smashstep.datn.employee.util;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.employee.dto.NhanVienRequest;
import com.smashstep.datn.employee.dto.VaiTroResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import static com.smashstep.datn.employee.dto.NhanVienImportDto.*;
@Component
public class NhanVienImportExcelReader {
    public static final int MAX_ROWS=500;
    public static final long MAX_BYTES=5L*1024*1024;
    public static final String[] HEADERS={"Họ và tên","Email","Số điện thoại","Giới tính","Ngày sinh (dd/MM/yyyy)","Vai trò (ID)","Tỉnh/Thành phố","Phường/Xã","Địa chỉ cụ thể","Hình ảnh (URL)"};
    private static final DateTimeFormatter DOB=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public List<CheckedRow> read(MultipartFile file) {
        if(file==null || file.isEmpty())throw AppException.badRequest("Vui lòng chọn file Excel");
        String name=String.valueOf(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if(!(name.endsWith(".xlsx")||name.endsWith(".xls")))throw AppException.badRequest("Chỉ nhận file .xlsx hoặc .xls");
        if(file.getSize()>MAX_BYTES)throw AppException.badRequest("File Excel tối đa 5 MB");
        try(InputStream input=file.getInputStream();Workbook book=WorkbookFactory.create(input)) {
            if(book.getNumberOfSheets()==0)throw AppException.badRequest("File không có bảng dữ liệu");
            Sheet sheet=book.getSheetAt(0);DataFormatter formatter=new DataFormatter(Locale.ROOT);
            org.apache.poi.ss.usermodel.Row header=sheet.getRow(0);
            for(int col=0;col<HEADERS.length;col++) if(header==null || !HEADERS[col].equals(formatter.formatCellValue(header.getCell(col)).trim())) throw AppException.badRequest("Cột "+(col+1)+" phải là '"+HEADERS[col]+"'. Hãy tải file mẫu.");
            if(sheet.getLastRowNum()>MAX_ROWS)throw AppException.badRequest("Tối đa 500 dòng dữ liệu; xóa các dòng thừa ở cuối file");
            List<CheckedRow> result=new ArrayList<>();
            for(int row=1;row<=sheet.getLastRowNum();row++) {
                org.apache.poi.ss.usermodel.Row line=sheet.getRow(row);if(line==null)continue;
                boolean blank=true;for(int col=0;col<HEADERS.length;col++)if(!formatter.formatCellValue(line.getCell(col)).isBlank()) {blank=false;break;}
                if(blank)continue;
                List<String> errors=new ArrayList<>();String[] values=new String[HEADERS.length];
                for(int col=0;col<values.length;col++) {
                    Cell cell=line.getCell(col);
                    if(cell!=null && (cell.getCellType()==CellType.FORMULA||cell.getCellType()==CellType.ERROR)) {errors.add(HEADERS[col]+": không nhận công thức hoặc ô lỗi");values[col]="";}
                    else values[col]=formatter.formatCellValue(cell).trim();
                }
                NhanVienRequest r=new NhanVienRequest();r.setName(values[0]);r.setEmail(values[1]);r.setPhone(values[2]);
                r.setGender(gender(values[3],errors));r.setDob(date(line.getCell(4),values[4],errors));
                if(!values[5].isBlank())try {r.setRoleId(Long.valueOf(values[5]));}catch(NumberFormatException e){errors.add("Vai trò phải là ID trong sheet Vai trò");}
                r.setProvince(values[6]);r.setWard(values[7]);r.setStreet(values[8]);r.setImage(values[9].isBlank()?null:values[9]);
                result.add(new CheckedRow(row+1,r,errors));
            }
            if(result.isEmpty())throw AppException.badRequest("File chưa có nhân viên để nhập");
            return result;
        } catch(AppException e){throw e;}catch(Exception e){throw AppException.badRequest("Không đọc được file Excel. File có thể hỏng, được bảo vệ hoặc không đúng định dạng.");}
    }
    static Integer gender(String value,List<String> errors) {
        if(value.isBlank())return null;
        return switch(value.toLowerCase(Locale.ROOT)) {case "0","khác","khac"->0;case "1","nam"->1;case "2","nữ","nu"->2;default->{errors.add("Giới tính: dùng Nam/Nữ/Khác hoặc 1/2/0");yield null;}};
    }
    static LocalDate date(Cell cell,String value,List<String> errors) {
        if(value.isBlank())return null;
        try {
            if(cell!=null && cell.getCellType()==CellType.NUMERIC) {
                double n=cell.getNumericCellValue();if(n<1||n>2958465)throw new DateTimeException("invalid");return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            return value.matches("\\d{4}-\\d{2}-\\d{2}")?LocalDate.parse(value):LocalDate.parse(value,DOB);
        }catch(Exception e){errors.add("Ngày sinh không hợp lệ; dùng dd/MM/yyyy (ví dụ 20/05/2000)");return null;}
    }
    public byte[] template(List<VaiTroResponse> roles) {
        try(Workbook book=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Sheet data=book.createSheet("Nhân viên");var header=data.createRow(0);
            CellStyle heading=book.createCellStyle();Font font=book.createFont();font.setBold(true);heading.setFont(font);heading.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle text=book.createCellStyle();text.setDataFormat(book.createDataFormat().getFormat("@"));
            for(int col=0;col<HEADERS.length;col++){Cell cell=header.createCell(col);cell.setCellValue(HEADERS[col]);cell.setCellStyle(heading);data.setColumnWidth(col,(col==0||col==1||col==8?30:23)*256);data.setDefaultColumnStyle(col,text);}
            data.createFreezePane(0,1);
            Sheet guide=book.createSheet("Hướng dẫn");
            String[] notes={"Mỗi dòng trong sheet Nhân viên là một người. Tối đa 500 dòng, file tối đa 5 MB.","Bắt buộc: họ tên, email, số điện thoại, giới tính, ngày sinh, vai trò, tỉnh/thành và phường/xã.","Số điện thoại lưu dạng Text để giữ số 0 đầu; phải gồm 10 số.","Ngày sinh: dd/MM/yyyy hoặc ô ngày Excel. Nhân viên phải đủ 18 tuổi.","Vai trò: điền ID đang hoạt động từ sheet Vai trò. Giới tính: Nam/Nữ/Khác hoặc 1/2/0.","Địa chỉ gồm Tỉnh/Thành phố + Phường/Xã; không nhập quận/huyện. Địa chỉ cụ thể và ảnh URL không bắt buộc.","Không nhập mã nhân viên, tên tài khoản hay mật khẩu. Hệ thống tự tạo.","Không dùng công thức. Chỉ nhập sau khi tất cả dòng hợp lệ và bấm Xác nhận nhập.","Email gửi sau khi lưu thành công. Email lỗi gửi lại tại Lịch sử nhập, không tạo lại nhân viên."};
            for(int i=0;i<notes.length;i++)guide.createRow(i).createCell(0).setCellValue(notes[i]);guide.setColumnWidth(0,120*256);
            Sheet roleSheet=book.createSheet("Vai trò");roleSheet.createRow(0).createCell(0).setCellValue("ID vai trò");roleSheet.getRow(0).createCell(1).setCellValue("Tên vai trò");
            for(int i=0;i<roles.size();i++){var row=roleSheet.createRow(i+1);row.createCell(0).setCellValue(roles.get(i).getId());row.createCell(1).setCellValue(roles.get(i).getName());}roleSheet.setColumnWidth(0,18*256);roleSheet.setColumnWidth(1,30*256);
            book.write(out);return out.toByteArray();
        }catch(IOException e){throw AppException.badRequest("Không tạo được file mẫu");}
    }
}
