package com.smashstep.datn.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

public final class DuLieuSanPham {
    private DuLieuSanPham() {}

    // Endpoint riêng lấy ID sản phẩm từ body.
    public interface BienTheDocLap extends jakarta.validation.groups.Default {}

    // Giữ cả body một biến thể và body {variants:[...]}.
    @Getter
    public static class DanhSachBienTheRequest {
        @NotEmpty(message = "Danh sách biến thể không được để trống.") @Size(max = 200, message = "Mỗi lần tạo tối đa 200 biến thể.")
        private final List<@NotNull(message = "Danh sách biến thể không được để trống.") @Valid SanPhamChiTietThemRequest> variants;
        private final boolean hangLoat;

        @JsonCreator
        public DanhSachBienTheRequest(@JsonProperty("variants") List<SanPhamChiTietThemRequest> variants,
                @JsonProperty("sanPhamId") @JsonAlias({"idSanPham", "productId"}) Object sanPhamId,
                @JsonProperty("maChiTietSanPham") String maChiTietSanPham, @JsonProperty("sku") String sku,
                @JsonProperty("mauSacId") @JsonAlias("idMauSac") Object mauSacId,
                @JsonProperty("kichThuocId") @JsonAlias("idKichThuoc") Object kichThuocId,
                @JsonProperty("soLuong") Object soLuong, @JsonProperty("giaBan") BigDecimal giaBan,
                @JsonProperty("kichHoat") Boolean kichHoat, @JsonProperty("trangThai") Object trangThai) {
            hangLoat = variants != null;
            SanPhamChiTietThemRequest bienThe = new SanPhamChiTietThemRequest(SoLuongSanPham.docId(sanPhamId), maChiTietSanPham, sku, SoLuongSanPham.docId(mauSacId), SoLuongSanPham.docId(kichThuocId),
                    SoLuongSanPham.doc(soLuong), giaBan, kichHoat, SoLuongSanPham.docTrangThai(trangThai));
            bienThe.setMaChiTietSanPham(maChiTietSanPham);
            bienThe.setSku(sku);
            this.variants = hangLoat ? variants : List.of(bienThe);
        }
    }

    @Data
    @NoArgsConstructor(onConstructor_ = @JsonCreator)
    @AllArgsConstructor
    public static class TrangThaiRequest {
        @NotNull(message = "Trạng thái không được để trống.") @Min(value = 0, message = "Trạng thái phải là 0 hoặc 1.") @Max(value = 1, message = "Trạng thái phải là 0 hoặc 1.")
        private Integer trangThai;

        @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
        public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }
    }

    @Data
    @NoArgsConstructor(onConstructor_ = @JsonCreator)
    @AllArgsConstructor
    public static class ThuocTinhRequest {
        // Giữ field để tương thích client cũ; service tự sinh mã và giữ mã khi sửa.
        @JsonAlias({"maDanhMuc", "maThuongHieu", "maChatLieu", "maKieuDang", "maCoGiay", "maXuatXu", "maMauSac"})
        private String ma;
        @NotBlank(message = "Tên/giá trị không được trống")
        @Size(max = 255, message = "Tên/giá trị thuộc tính tối đa 255 ký tự.")
        @JsonAlias({"tenDanhMuc", "tenThuongHieu", "tenChatLieu", "tenKieuDang", "tenCoGiay", "tenXuatXu", "tenMauSac", "giaTri"})
        private String ten;
        @Size(max = 1000, message = "Mô tả/ghi chú tối đa 1000 ký tự.")
        @JsonAlias("moTa")
        private String ghiChu;
        @Size(max = 20, message = "Mã HEX tối đa 20 ký tự.")
        private String maMauHex;
        @NotNull(message = "Trạng thái không được để trống.") @Min(value = 0, message = "Trạng thái phải là 0 hoặc 1.") @Max(value = 1, message = "Trạng thái phải là 0 hoặc 1.")
        private Integer trangThai;

        @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
        public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThuocTinhResponse {
        private Long id;
        private String ma;
        private String ten;
        private String ghiChu;
        private String maMauHex;
        private Integer trangThai;

        @com.fasterxml.jackson.annotation.JsonSetter("trangThai")
        public void setTrangThai(Object value) { trangThai = SoLuongSanPham.docTrangThai(value); }
    }

}
