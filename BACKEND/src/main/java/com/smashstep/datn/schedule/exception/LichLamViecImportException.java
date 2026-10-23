package com.smashstep.datn.schedule.exception;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.dto.LichLamViecImportDto.XemTruoc;
import org.springframework.http.HttpStatus;
public class LichLamViecImportException extends AppException {
    private final XemTruoc data;
    public LichLamViecImportException(XemTruoc data){super(HttpStatus.CONFLICT,"Dữ liệu có lỗi hoặc vừa thay đổi. Kiểm tra lại các dòng trước khi nhập.");this.data=data;}
    public XemTruoc getData(){return data;}
}
