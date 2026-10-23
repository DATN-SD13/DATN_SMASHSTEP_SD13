package com.smashstep.datn.employee.exeption;
import com.smashstep.datn.employee.dto.NhanVienImportDto;
public class NhanVienImportException extends RuntimeException {
    private final NhanVienImportDto.Preview preview;
    public NhanVienImportException(NhanVienImportDto.Preview preview){super("Dữ liệu đã thay đổi hoặc có dòng không hợp lệ. Chưa tạo nhân viên nào.");this.preview=preview;}
    public NhanVienImportDto.Preview getPreview(){return preview;}
}
