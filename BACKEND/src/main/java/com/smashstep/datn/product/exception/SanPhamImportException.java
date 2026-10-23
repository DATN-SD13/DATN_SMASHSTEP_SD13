package com.smashstep.datn.product.exception;
import com.smashstep.datn.product.dto.SanPhamImportDto.XemTruoc;
public class SanPhamImportException extends RuntimeException {
    private final XemTruoc xemTruoc;
    public SanPhamImportException(XemTruoc value) { super("Có dòng không hợp lệ hoặc dữ liệu đã thay đổi. Chưa lưu sản phẩm nào."); xemTruoc=value; }
    public XemTruoc getXemTruoc() { return xemTruoc; }
}
