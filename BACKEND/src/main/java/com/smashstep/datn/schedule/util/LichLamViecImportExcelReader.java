package com.smashstep.datn.schedule.util;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.dto.LichLamViecImportDto.Dong;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.*;

@Component
public class LichLamViecImportExcelReader {
    public static final long MAX_BYTES=5L*1024*1024;
    public static final List<String> COT=List.of("maNhanVien (*)","Tên nhân viên (tự động)","maCa (*)",
            "Tên ca (tự động)","ngayLam yyyy-MM-dd (*)","ghiChu");
    public record DocDong(Dong dong,List<String> loi) {}
    public List<DocDong> doc(MultipartFile file) {
        if(file==null || file.isEmpty())throw AppException.badRequest("Vui lòng chọn file Excel");
        if(file.getSize()>MAX_BYTES)throw AppException.badRequest("File Excel tối đa 5 MB");
        String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
        if(!name.endsWith(".xlsx")&&!name.endsWith(".xls"))throw AppException.badRequest("Chỉ nhận file .xlsx hoặc .xls");
        try(var input=file.getInputStream();var book=WorkbookFactory.create(input)){
            if(book.getNumberOfSheets()==0)throw AppException.badRequest("File Excel không có sheet");
            var sheet=book.getSheet("LichLamViec");if(sheet==null)sheet=book.getSheetAt(0);
            var formatter=new DataFormatter(Locale.ROOT);var header=sheet.getRow(0);
            for(int c=0;c<COT.size();c++)if(header==null||!COT.get(c).equals(formatter.formatCellValue(header.getCell(c)).trim()))
                throw AppException.badRequest("Tiêu đề cột "+(c+1)+" phải là '"+COT.get(c)+"'. Hãy tải mẫu mới.");
            for(int c=COT.size();c<header.getLastCellNum();c++)if(!formatter.formatCellValue(header.getCell(c)).isBlank())
                throw AppException.badRequest("File chỉ được có 6 cột theo mẫu");
            if(sheet.getLastRowNum()>5000)throw AppException.badRequest("File có quá nhiều dòng trống/định dạng thừa. Xóa dòng thừa rồi nhập lại.");
            var result=new ArrayList<DocDong>();
            for(int i=1;i<=sheet.getLastRowNum();i++){
                var row=sheet.getRow(i);if(row==null)continue;var errors=new ArrayList<String>();
                String nv=cell(row.getCell(0),0,errors),ca=cell(row.getCell(2),2,errors);
                String date=cell(row.getCell(4),4,errors),note=cell(row.getCell(5),5,errors);
                // Old templates prefilled dates on all 500 rows. Ignore untouched rows, including lookup formulas.
                if(nv.isBlank()&&ca.isBlank()&&note.isBlank()&&errors.isEmpty())continue;
                result.add(new DocDong(new Dong(i+1,nv,ca,date,note),errors));
                if(result.size()>500)throw AppException.badRequest("Tối đa 500 dòng dữ liệu mỗi lần nhập");
            }
            if(result.isEmpty())throw AppException.badRequest("Chưa có lịch làm việc để nhập");
            return result;
        }catch(AppException e){throw e;}catch(Exception e){throw AppException.badRequest("Không đọc được Excel: file hỏng, được bảo vệ hoặc sai định dạng");}
    }
    private String cell(Cell cell,int column,List<String> errors){
        if(cell==null)return "";
        if(cell.getCellType()==CellType.FORMULA||cell.getCellType()==CellType.ERROR){
            errors.add(COT.get(column)+": không nhận công thức hoặc ô lỗi");return "";
        }
        String value;
        if(cell.getCellType()==CellType.NUMERIC){
            if(column==4&&DateUtil.isCellDateFormatted(cell))value=cell.getLocalDateTimeCellValue().toLocalDate().toString();
            else value=BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }else value=new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
        if(value.length()>10000){errors.add(COT.get(column)+": nội dung quá dài");value=value.substring(0,10000);}
        return value;
    }
}
