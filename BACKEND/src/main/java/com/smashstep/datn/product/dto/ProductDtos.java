package com.smashstep.datn.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** API only returns DTOs; lazy JPA relationships stay inside the transaction. */
public final class ProductDtos {
    private ProductDtos() {}

    public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }

    public record ProductRequest(
            @NotBlank(message="Mã sản phẩm không được trống") @Size(max=50)
            @Pattern(regexp="^[A-Za-z0-9][A-Za-z0-9._-]*$", message="Mã chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang") String maSanPham,
            @NotBlank(message="Tên sản phẩm không được trống") @Size(max=255) String tenSanPham,
            @NotNull @Positive Long danhMucId, @NotNull @Positive Long thuongHieuId,
            @NotNull @Positive Long chatLieuId, @NotNull @Positive Long kieuDangId,
            @NotNull @Positive Long coGiayId, @NotNull @Positive Long xuatXuId,
            String moTaChiTiet, @NotNull @Min(0) @Max(1) Integer trangThai) {}

    public record ProductResponse(
            Long id, String maSanPham, String tenSanPham,
            Long danhMucId, String tenDanhMuc, Long thuongHieuId, String tenThuongHieu,
            Long chatLieuId, String tenChatLieu, Long kieuDangId, String tenKieuDang,
            Long coGiayId, String tenCoGiay, Long xuatXuId, String tenXuatXu,
            String moTaChiTiet, Integer trangThai, LocalDateTime ngayTao, LocalDateTime ngayCapNhat,
            long tongSoLuong, BigDecimal giaThapNhat, BigDecimal giaCaoNhat, long tongSoBienThe) {}

    public record ProductDetail(ProductResponse product, List<VariantResponse> variants, List<ImageResponse> images) {}

    public record VariantRequest(
            @NotNull @Positive Long sanPhamId,
            @NotBlank(message="Mã biến thể không được trống") @Size(max=100)
            @Pattern(regexp="^[A-Za-z0-9][A-Za-z0-9._-]*$") String maChiTietSanPham,
            @NotBlank(message="SKU không được trống") @Size(max=100)
            @Pattern(regexp="^[A-Za-z0-9][A-Za-z0-9._-]*$") String sku,
            @NotNull @Positive Long mauSacId, @NotNull @Positive Long kichThuocId,
            @NotNull @Min(value=0, message="Số lượng phải từ 0 trở lên") Integer soLuong,
            @NotNull @DecimalMin(value="0", inclusive=false, message="Giá bán phải lớn hơn 0")
            @Digits(integer=16, fraction=2, message="Giá bán tối đa 16 chữ số và 2 số thập phân") BigDecimal giaBan,
            @NotNull Boolean kichHoat, @NotNull @Min(0) @Max(1) Integer trangThai) {}

    public record VariantBatchRequest(@NotEmpty @Size(max=200) List<@Valid VariantRequest> variants) {}
    public record VariantResponse(Long id, Long sanPhamId, String maSanPham, String tenSanPham,
            String maChiTietSanPham, String sku, Long mauSacId, String tenMauSac, String maMauHex,
            Long kichThuocId, String giaTriKichThuoc, Integer soLuong, BigDecimal giaBan,
            Boolean kichHoat, Integer trangThai, LocalDateTime ngayTao, LocalDateTime ngayCapNhat) {}

    public record StatusRequest(@NotNull @Min(0) @Max(1) Integer trangThai) {}

    public record AttributeRequest(@Size(max=50) String ma, @NotBlank(message="Tên/giá trị không được trống")
            @Size(max=255) String ten, @Size(max=1000) String ghiChu, @Size(max=20) String maMauHex,
            @NotNull @Min(0) @Max(1) Integer trangThai) {}
    public record AttributeResponse(Long id, String ma, String ten, String ghiChu, String maMauHex, Integer trangThai) {}

    public record ImageRequest(@NotBlank(message="URL ảnh không được trống") @Size(max=1000) String urlAnh,
            @NotNull Boolean isAnhChinh) {}
    public record ImageResponse(Long id, String urlAnh, Boolean isAnhChinh) {}
}

