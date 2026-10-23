package com.smashstep.datn.product.util;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.SanPhamImportDto.*;
import com.smashstep.datn.product.dto.DuLieuSanPham.ThuocTinhResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

@Component
public class SanPhamImportExcelReader {
    public static final List<String> COT=List.of("Nhóm sản phẩm","Tên sản phẩm","Danh mục","Thương hiệu","Chất liệu","Kiểu dáng","Cổ giày","Xuất xứ","Mô tả","Trạng thái sản phẩm","Màu sắc","Kích thước","Số lượng","Giá bán","Kích hoạt","Trạng thái biến thể");
    // Excel contains only editable fields. Internal preview/confirmation keeps 16 fields.
    public static final List<Integer> VI_TRI_EXCEL=List.of(0,1,2,3,4,5,6,7,8,10,11,12,13);
    public static final List<String> COT_EXCEL=VI_TRI_EXCEL.stream().map(COT::get).toList();
    public static final long MAX_BYTES=5L*1024*1024;
    public List<KiemTraDong> doc(MultipartFile file) {
        if(file==null||file.isEmpty())throw AppException.badRequest("Vui lòng chọn file Excel");
        if(file.getSize()>MAX_BYTES)throw AppException.badRequest("File Excel tối đa 5 MB");
        String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
        if(!name.endsWith(".xlsx")&&!name.endsWith(".xls"))throw AppException.badRequest("Chỉ nhận file .xlsx hoặc .xls");
        try(var input=file.getInputStream();Workbook book=WorkbookFactory.create(input)) {
            if(book.getNumberOfSheets()==0)throw AppException.badRequest("File không có sheet dữ liệu");
            Sheet sheet=book.getSheetAt(0);DataFormatter format=new DataFormatter(Locale.ROOT);
            Row head=sheet.getRow(0);
            boolean compact=dungBoTieuDe(head,format,true);
            if(!compact&&!dungBoTieuDe(head,format,false))throw AppException.badRequest("Tiêu đề không đúng file mẫu sản phẩm. Hãy tải file mẫu mới (13 cột, không có trạng thái/kích hoạt).");
            List<Integer> positions=compact?VI_TRI_EXCEL:java.util.stream.IntStream.range(0,COT.size()).boxed().toList();
            if(sheet.getLastRowNum()>500)throw AppException.badRequest("Tối đa 500 dòng dữ liệu. Xóa dòng thừa ở cuối file.");
            var result=new ArrayList<KiemTraDong>();
            for(int i=1;i<=sheet.getLastRowNum();i++) {
                Row row=sheet.getRow(i);if(row==null)continue;
                boolean blank=true;var values=new ArrayList<String>(Collections.nCopies(COT.size(),""));var errors=new ArrayList<String>();
                for(int c=0;c<positions.size();c++) {
                    int position=positions.get(c);
                    Cell cell=row.getCell(c);String value=format.formatCellValue(cell).trim();
                    if(!value.isBlank()||(cell!=null&&cell.getCellType()==CellType.FORMULA))blank=false;
                    if(cell!=null&&(cell.getCellType()==CellType.FORMULA||cell.getCellType()==CellType.ERROR)) { errors.add(COT.get(position)+": không nhận công thức hoặc ô lỗi");value=""; }
                    else if(cell!=null&&cell.getCellType()==CellType.NUMERIC) value=BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
                    if(value.length()>10000){errors.add(COT.get(position)+": tối đa 10.000 ký tự");value=value.substring(0,10000);}
                    values.set(position,value);
                }
                if(!blank){
                    if(compact)for(int column:List.of(9,14,15))values.set(column,"1");
                    result.add(new KiemTraDong(i+1,List.copyOf(values),errors));
                }
            }
            if(result.isEmpty())throw AppException.badRequest("Chưa có sản phẩm để nhập");
            return result;
        }catch(AppException e){throw e;}catch(Exception e){throw AppException.badRequest("Không đọc được Excel. File có thể hỏng, được bảo vệ hoặc không đúng định dạng.");}
    }
    private boolean dungBoTieuDe(Row row,DataFormatter format,boolean compact) {
        if(row==null)return false;
        List<String> expected=compact?COT_EXCEL:COT;
        for(int i=0;i<expected.size();i++){
            String value=format.formatCellValue(row.getCell(i)).trim();
            boolean oldFlag=!compact&&(i==9||i==14||i==15)&&(expected.get(i)+" (0/1)").equals(value);
            if(!expected.get(i).equals(value)&&!oldFlag)return false;
        }
        for(int i=expected.size();i<row.getLastCellNum();i++)if(!format.formatCellValue(row.getCell(i)).isBlank())return false;
        return true;
    }
    public byte[] mau(Map<String,List<ThuocTinhResponse>> options) {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            Sheet sheet=book.createSheet("Sản phẩm");Row header=sheet.createRow(0);
            CellStyle heading=book.createCellStyle();Font font=book.createFont();font.setBold(true);heading.setFont(font);heading.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle text=book.createCellStyle();text.setDataFormat(book.createDataFormat().getFormat("@"));
            for(int c=0;c<COT_EXCEL.size();c++){var cell=header.createCell(c);cell.setCellValue(COT_EXCEL.get(c));cell.setCellStyle(heading);sheet.setColumnWidth(c,(c==1||c==8?32:24)*256);sheet.setDefaultColumnStyle(c,text);}
            sheet.createFreezePane(2,1);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,COT_EXCEL.size()-1));
            Sheet guide=book.createSheet("Hướng dẫn");String[] notes={
                    "Mỗi dòng là một biến thể. Tối đa 500 dòng / 5 MB. Nhập trên sheet đầu tiên Sản phẩm.",
                    "Các dòng cùng Nhóm sản phẩm (ví dụ NHOM01) tạo chung một sản phẩm MỚI; khác nhóm tạo sản phẩm khác.",
                    "Nhóm sản phẩm là nhãn trong file, không phải mã có sẵn. Mã SP, mã biến thể và SKU được tự sinh khi lưu.",
                    "Tên và 6 thuộc tính, mô tả phải giống nhau trong cùng nhóm.",
                    "Nhập TÊN thuộc tính theo các sheet kèm theo, ví dụ Nike, Đen; kích thước nhập 40 hoặc 41, KHÔNG nhập ID. Chỉ dùng thuộc tính đang hoạt động.",
                    "Tên được so khớp không phân biệt hoa thường, bỏ khoảng trắng thừa và giữ dấu tiếng Việt. Tên không tồn tại hoặc trùng nhiều thuộc tính sẽ báo lỗi. Một nhóm không được lặp cùng Màu sắc + Kích thước. Không nhập sản phẩm trùng tên và bộ thuộc tính đã có.",
                    "Số lượng là số nguyên từ 0 đến 2147483647. Giá > 0, tối đa 16 chữ số phần nguyên và 2 số thập phân.",
                    "Nhập giá ví dụ 1250000 hoặc 1250000.50. Không thêm dấu phân cách hàng nghìn hay ký hiệu tiền.",
                    "Excel không có cột trạng thái/kích hoạt. Khi nhập: trạng thái sản phẩm và biến thể là Hoạt động, kích hoạt là Có.",
                    "File gồm 13 cột. Mô tả được để trống; các cột khác bắt buộc. Không dùng công thức, không đổi tiêu đề.",
                    "Không cộng tồn hay ghi đè sản phẩm cũ. Ảnh sản phẩm thêm sau tại trang chi tiết/sửa sản phẩm.",
                    "Chọn file -> Xem trước -> sửa toàn bộ lỗi -> Xác nhận nhập. Có lỗi khi lưu sẽ rollback cả lô."
            };
            for(int i=0;i<notes.length;i++)guide.createRow(i).createCell(0).setCellValue(notes[i]);guide.setColumnWidth(0,150*256);
            String[] keys={"categories","brands","materials","styles","collars","origins","colors","sizes"};String[] names={"Danh mục","Thương hiệu","Chất liệu","Kiểu dáng","Cổ giày","Xuất xứ","Màu sắc","Kích thước"};
            for(int i=0;i<keys.length;i++){Sheet ref=book.createSheet(names[i]);ref.createRow(0).createCell(0).setCellValue("Tên / giá trị");int line=1;for(var a:options.getOrDefault(keys[i],List.of()))if(Integer.valueOf(1).equals(a.getTrangThai())){Row r=ref.createRow(line++);r.createCell(0).setCellValue(a.getTen());}ref.setColumnWidth(0,45*256);}
            book.write(out);return out.toByteArray();
        }catch(IOException e){throw AppException.badRequest("Không tạo được file mẫu");}
    }
}
