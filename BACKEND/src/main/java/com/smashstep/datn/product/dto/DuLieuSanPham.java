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
        @NotEmpty @Size(max = 200)
        private final List<@NotNull @Valid SanPhamChiTietThemRequest> variants;
        private final boolean hangLoat;

        @JsonCreator
        public DanhSachBienTheRequest(@JsonProperty("variants") List<SanPhamChiTietThemRequest> variants,
                @JsonProperty("sanPhamId") @JsonAlias({"idSanPham", "productId"}) Long sanPhamId,
                @JsonProperty("maChiTietSanPham") String maChiTietSanPham, @JsonProperty("sku") String sku,
                @JsonProperty("mauSacId") @JsonAlias("idMauSac") Long mauSacId,
                @JsonProperty("kichThuocId") @JsonAlias("idKichThuoc") Long kichThuocId,
                @JsonProperty("soLuong") Integer soLuong, @JsonProperty("giaBan") BigDecimal giaBan,
                @JsonProperty("kichHoat") Boolean kichHoat, @JsonProperty("trangThai") Integer trangThai) {
            hangLoat = variants != null;
            SanPhamChiTietThemRequest bienThe = new SanPhamChiTietThemRequest(sanPhamId, maChiTietSanPham, sku, mauSacId, kichThuocId,
                    soLuong, giaBan, kichHoat, trangThai);
            bienThe.setMaChiTietSanPham(maChiTietSanPham);
            bienThe.setSku(sku);
            this.variants = hangLoat ? variants : List.of(bienThe);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrangThaiRequest {
        @NotNull @Min(0) @Max(1)
        private Integer trangThai;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThuocTinhRequest {
        @Size(max = 50)
        @JsonAlias({"maDanhMuc", "maThuongHieu", "maChatLieu", "maKieuDang", "maCoGiay", "maXuatXu", "maMauSac"})
        private String ma;
        @NotBlank(message = "Tên/giá trị không được trống")
        @Size(max = 255)
        @JsonAlias({"tenDanhMuc", "tenThuongHieu", "tenChatLieu", "tenKieuDang", "tenCoGiay", "tenXuatXu", "tenMauSac", "giaTri"})
        private String ten;
        @Size(max = 1000)
        @JsonAlias("moTa")
        private String ghiChu;
        @Size(max = 20)
        private String maMauHex;
        @NotNull @Min(0) @Max(1)
        private Integer trangThai;
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
    }

}
