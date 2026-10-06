package com.smashstep.datn.promotion.service;

import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.product.repository.SanPhamChiTietRepository;
import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import com.smashstep.datn.promotion.entity.DotGiamGia;
import com.smashstep.datn.promotion.repository.ChiTietDotGiamGiaRepository;
import com.smashstep.datn.promotion.repository.DotGiamGiaRepository;
import com.smashstep.datn.promotion.request.DotGiamGiaRequest;
import com.smashstep.datn.promotion.response.DotGiamGiaResponse;
import com.smashstep.datn.promotion.specification.DotGiamGiaSpecification;
import com.smashstep.datn.promotion.response.SanPhamChiTietGiamGiaResponse;
import com.smashstep.datn.common.exception.AppException;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DotGiamGiaService {

    private final DotGiamGiaRepository dotGiamGiaRepository;

    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;

    private final SanPhamChiTietRepository sanPhamChiTietRepository;


    // =====================================================
    // DANH SÁCH + TÌM KIẾM + LỌC + PHÂN TRANG
    // =====================================================

    public Page<DotGiamGiaResponse> getAll(
            String ma,
            LocalDate tuNgay,
            LocalDate denNgay,
            Integer trangThai,
            int page,
            int size
    ) {

        // FE sử dụng page bắt đầu từ 1
        int pageIndex = Math.max(page - 1, 0);

        Pageable pageable = PageRequest.of(
                pageIndex,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return dotGiamGiaRepository
                .findAll(
                        DotGiamGiaSpecification.filter(
                                ma,
                                tuNgay,
                                denNgay,
                                trangThai
                        ),
                        pageable
                )
                .map(this::toResponse);
    }


    // =====================================================
    // XEM CHI TIẾT THEO MÃ
    // =====================================================
    public DotGiamGiaResponse getByMa(String ma) {

        DotGiamGia dotGiamGia =
                dotGiamGiaRepository
                        .findByMaDotGiamGia(ma)
                        .orElseThrow(() ->
                                AppException.notFound(
                                        "Không tìm thấy đợt giảm giá: " + ma
                                )
                        );

        // Không cho xem đợt giảm giá đã xóa mềm
        if (Integer.valueOf(-1)
                .equals(dotGiamGia.getTrangThai())) {

            throw AppException.notFound(
                    "Không tìm thấy đợt giảm giá: " + ma
            );
        }

        return toResponse(dotGiamGia);
    }


    // =====================================================
    // TẠO ĐỢT GIẢM GIÁ
    // =====================================================
    private String generateCode() {

        DotGiamGia latest =
                dotGiamGiaRepository
                        .findTopByOrderByIdDesc()
                        .orElse(null);

        if (latest == null
                || latest.getMaDotGiamGia() == null) {
            return "DGG001";
        }

        String latestCode =
                latest.getMaDotGiamGia();

        try {
            int number = Integer.parseInt(
                    latestCode.replace("DGG", "")
            );

            return String.format(
                    "DGG%03d",
                    number + 1
            );

        } catch (NumberFormatException e) {
            return "DGG001";
        }
    }
    @Transactional
    public DotGiamGiaResponse create(
            DotGiamGiaRequest request
    ) {

        // -----------------------------
// 1. Tự động sinh mã
// -----------------------------
        String code = generateCode();


        // -----------------------------
        // 3. Kiểm tra ngày
        // -----------------------------

        if (request.getEndDate()
                .isBefore(request.getStartDate())) {

            throw AppException.badRequest(
                    "Ngày kết thúc không được nhỏ hơn ngày bắt đầu"
            );
        }

        validateProductPromotionConflict(
                request.getProductDetailIds(),
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        // -----------------------------
        // 4. Tạo đợt giảm giá
        // -----------------------------

        LocalDateTime now =
                LocalDateTime.now();

        DotGiamGia entity =
                new DotGiamGia();

        entity.setMaDotGiamGia(
                code
        );

        entity.setTenDotGiamGia(
                request.getName().trim()
        );

        entity.setPhanTramGiamDot(
                request.getDiscountValue()
        );

        entity.setNgayBatDau(
                request
                        .getStartDate()
                        .atStartOfDay()
        );

        entity.setNgayKetThuc(
                request
                        .getEndDate()
                        .atTime(23, 59, 59)
        );

        entity.setMoTa(
                request.getDescription()
        );

        entity.setKichHoat(true);

        // Theo block F SQL:
        // 1 = Hoạt động
        entity.setTrangThai(1);

        entity.setNgayTao(now);
        entity.setNgayCapNhat(now);


        // -----------------------------
        // 5. Lưu đợt giảm giá
        // -----------------------------

        DotGiamGia saved =
                dotGiamGiaRepository.save(entity);


        // -----------------------------
        // 6. Lưu biến thể sản phẩm
        // -----------------------------

        if (request.getProductDetailIds() != null
                && !request
                .getProductDetailIds()
                .isEmpty()) {

            for (Long productDetailId
                    : request.getProductDetailIds()) {


                // Tìm biến thể sản phẩm
                SanPhamChiTiet sanPhamChiTiet =
                        sanPhamChiTietRepository
                                .findById(productDetailId)
                                .orElseThrow(() ->
                                        AppException.notFound(
                                                "Không tìm thấy biến thể sản phẩm ID: "
                                                        + productDetailId
                                        )
                                );


                // Kiểm tra biến thể có bị gửi trùng không
                boolean daTonTai =
                        chiTietDotGiamGiaRepository
                                .existsByIdDotGiamGia_IdAndIdSanPhamChiTiet_Id(
                                        saved.getId(),
                                        productDetailId
                                );


                if (daTonTai) {
                    continue;
                }


                // Tạo chi tiết đợt giảm giá
                ChiTietDotGiamGia chiTiet =
                        new ChiTietDotGiamGia();

                chiTiet.setIdDotGiamGia(
                        saved
                );

                chiTiet.setIdSanPhamChiTiet(
                        sanPhamChiTiet
                );

                // Biến thể dùng % giảm của đợt
                chiTiet.setPhanTramGiamBienThe(
                        request.getDiscountValue()
                );

                // 1 = Hoạt động
                chiTiet.setTrangThai(1);

                chiTiet.setNgayTao(now);


                // Lưu chi tiết
                chiTietDotGiamGiaRepository
                        .save(chiTiet);
            }
        }


        // -----------------------------
        // 7. Trả dữ liệu cho FE
        // -----------------------------

        return toResponse(saved);
    }

// =====================================================
// DANH SÁCH BIẾN THỂ SẢN PHẨM CHO ĐỢT GIẢM GIÁ
// =====================================================

    public List<SanPhamChiTietGiamGiaResponse> getProductDetails(
            String keyword
    ) {

        return sanPhamChiTietRepository
                .findAll()
                .stream()

                // Chỉ lấy biến thể đang hoạt động
                .filter(spct ->
                        spct.getTrangThai() != null
                                && spct.getTrangThai() == 1
                )

                // Tìm kiếm
                .filter(spct -> {

                    if (keyword == null
                            || keyword.trim().isEmpty()) {
                        return true;
                    }

                    String search =
                            keyword.trim().toLowerCase();

                    String productCode =
                            spct.getIdSanPham() == null
                                    ? ""
                                    : spct.getIdSanPham()
                                    .getMaSanPham()
                                    .toLowerCase();

                    String productName =
                            spct.getIdSanPham() == null
                                    ? ""
                                    : spct.getIdSanPham()
                                    .getTenSanPham()
                                    .toLowerCase();

                    String detailCode =
                            spct.getMaChiTietSanPham() == null
                                    ? ""
                                    : spct.getMaChiTietSanPham()
                                    .toLowerCase();

                    String sku =
                            spct.getSku() == null
                                    ? ""
                                    : spct.getSku()
                                    .toLowerCase();

                    return productCode.contains(search)
                            || productName.contains(search)
                            || detailCode.contains(search)
                            || sku.contains(search);
                })

                .map(this::toProductDetailResponse)
                .toList();
    }


// =====================================================
// SAN PHAM CHI TIET -> RESPONSE
// =====================================================

    private SanPhamChiTietGiamGiaResponse toProductDetailResponse(
            SanPhamChiTiet spct
    ) {

        return SanPhamChiTietGiamGiaResponse
                .builder()

                .id(spct.getId())

                .productCode(
                        spct.getIdSanPham() == null
                                ? null
                                : spct.getIdSanPham().getMaSanPham()
                )

                .productName(
                        spct.getIdSanPham() == null
                                ? null
                                : spct.getIdSanPham().getTenSanPham()
                )

                .productDetailCode(
                        spct.getMaChiTietSanPham()
                )

                .sku(
                        spct.getSku()
                )

                .color(
                        spct.getIdMauSac() == null
                                ? null
                                : spct.getIdMauSac().getTenMauSac()
                )

                .colorHex(
                        spct.getIdMauSac() == null
                                ? null
                                : spct.getIdMauSac().getMaMauHex()
                )

                .size(
                        spct.getIdKichThuoc() == null
                                ? null
                                : spct.getIdKichThuoc().getGiaTri()
                )

                .price(
                        spct.getGiaBan()
                )

                .quantity(
                        spct.getSoLuong()
                )

                .status(
                        spct.getTrangThai()
                )

                .build();
    }
    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================
    @Transactional
    public DotGiamGiaResponse update(
            String ma,
            DotGiamGiaRequest request
    ) {

        // 1. Tìm đợt giảm giá cần sửa
        DotGiamGia entity = dotGiamGiaRepository
                .findByMaDotGiamGia(ma)
                .orElseThrow(() ->
                        AppException.notFound(
                                "Không tìm thấy đợt giảm giá: " + ma
                        )
                );
        // Không cho sửa đợt giảm giá đã xóa mềm
        if (Integer.valueOf(-1).equals(entity.getTrangThai())) {
            throw AppException.notFound(
                    "Không tìm thấy đợt giảm giá: " + ma
            );
        }

        // 2. Kiểm tra ngày
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw AppException.badRequest(
                    "Ngày kết thúc không được nhỏ hơn ngày bắt đầu"
            );
        }
        validateProductPromotionConflict(
                request.getProductDetailIds(),
                request.getStartDate(),
                request.getEndDate(),
                entity.getId()
        );

        // 3. Chuẩn hóa mã mới


        // 5. Cập nhật thông tin

        entity.setTenDotGiamGia(
                request.getName().trim()
        );

        entity.setPhanTramGiamDot(
                request.getDiscountValue()
        );

        entity.setNgayBatDau(
                request.getStartDate().atStartOfDay()
        );

        entity.setNgayKetThuc(
                request.getEndDate().atTime(23, 59, 59)
        );

        entity.setMoTa(
                request.getDescription()
        );

        entity.setNgayCapNhat(
                LocalDateTime.now()
        );

        // 6. Lưu
        DotGiamGia saved =
                dotGiamGiaRepository.save(entity);


// =====================================================
// CẬP NHẬT DANH SÁCH BIẾN THỂ SẢN PHẨM
// =====================================================

        if (request.getProductDetailIds() != null) {

            // Xóa danh sách sản phẩm cũ của đợt giảm giá
            chiTietDotGiamGiaRepository
                    .deleteByDotGiamGiaId(saved.getId());

            chiTietDotGiamGiaRepository.flush();
            // Thêm lại danh sách mới
            for (Long productDetailId :
                    request.getProductDetailIds()
                            .stream()
                            .distinct()
                            .toList()) {

                SanPhamChiTiet sanPhamChiTiet =
                        sanPhamChiTietRepository
                                .findById(productDetailId)
                                .orElseThrow(() ->
                                        AppException.notFound(
                                                "Không tìm thấy biến thể sản phẩm ID: "
                                                        + productDetailId
                                        )
                                );

                ChiTietDotGiamGia chiTiet =
                        new ChiTietDotGiamGia();

                chiTiet.setIdDotGiamGia(saved);

                chiTiet.setIdSanPhamChiTiet(
                        sanPhamChiTiet
                );

                chiTiet.setPhanTramGiamBienThe(
                        request.getDiscountValue()
                );

                chiTiet.setTrangThai(
                        Integer.valueOf(1).equals(saved.getTrangThai())
                                ? 1
                                : 0
                );

                chiTiet.setNgayTao(
                        LocalDateTime.now()
                );

                chiTietDotGiamGiaRepository.save(chiTiet);
            }
        }

        return toResponse(saved);
    }
    @Transactional
    public DotGiamGiaResponse delete(String ma) {

        DotGiamGia entity = dotGiamGiaRepository
                .findByMaDotGiamGia(ma)
                .orElseThrow(() ->
                        AppException.notFound(
                                "Không tìm thấy đợt giảm giá: " + ma
                        )
                );

        // Nếu đã xóa rồi thì không cho xóa lần nữa
        if (Integer.valueOf(-1).equals(entity.getTrangThai())) {
            throw AppException.badRequest(
                    "Đợt giảm giá đã được xóa"
            );
        }

        // ==========================
        // XÓA MỀM
        // -1 = Đã xóa
        // ==========================
        entity.setTrangThai(-1);
        entity.setKichHoat(false);
        entity.setNgayCapNhat(LocalDateTime.now());

        DotGiamGia saved =
                dotGiamGiaRepository.save(entity);

        // ==========================
        // VÔ HIỆU HÓA CHI TIẾT
        // ==========================
        List<ChiTietDotGiamGia> chiTietList =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGia_Id(saved.getId());

        for (ChiTietDotGiamGia chiTiet : chiTietList) {
            chiTiet.setTrangThai(0);
        }

        chiTietDotGiamGiaRepository.saveAll(
                chiTietList
        );

        return toResponse(saved);
    }

    private void validateProductPromotionConflict(
            List<Long> productDetailIds,
            LocalDate startDate,
            LocalDate endDate,
            Long currentPromotionId
    ) {

        if (productDetailIds == null || productDetailIds.isEmpty()) {
            return;
        }

        for (Long productDetailId :
                productDetailIds.stream().distinct().toList()) {

            List<ChiTietDotGiamGia> activePromotions =
                    chiTietDotGiamGiaRepository
                            .findActiveByProductDetailId(productDetailId);

            for (ChiTietDotGiamGia chiTiet : activePromotions) {

                DotGiamGia otherPromotion =
                        chiTiet.getIdDotGiamGia();

                // Khi UPDATE:
                // bỏ qua chính đợt giảm giá đang sửa
                if (currentPromotionId != null
                        && otherPromotion.getId()
                        .equals(currentPromotionId)) {
                    continue;
                }

                LocalDate otherStart =
                        otherPromotion
                                .getNgayBatDau()
                                .toLocalDate();

                LocalDate otherEnd =
                        otherPromotion
                                .getNgayKetThuc()
                                .toLocalDate();

                // Hai khoảng thời gian bị giao nhau khi:
                // start <= otherEnd
                // và end >= otherStart
                boolean overlap =
                        !startDate.isAfter(otherEnd)
                                && !endDate.isBefore(otherStart);

                if (overlap) {
                    throw AppException.conflict(
                            "Biến thể sản phẩm ID "
                                    + productDetailId
                                    + " đang thuộc đợt giảm giá "
                                    + otherPromotion.getMaDotGiamGia()
                                    + " trong khoảng "
                                    + otherStart
                                    + " đến "
                                    + otherEnd
                    );
                }
            }
        }
    }

    private DotGiamGiaResponse toResponse(DotGiamGia entity) {

        List<Long> productDetailIds =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGia_Id(entity.getId())
                        .stream()
                        .map(ct ->
                                ct.getIdSanPhamChiTiet().getId()
                        )
                        .toList();

        return DotGiamGiaResponse.builder()
                .id(entity.getId())
                .code(entity.getMaDotGiamGia())
                .name(entity.getTenDotGiamGia())
                .discountValue(entity.getPhanTramGiamDot())

                .startDate(
                        entity.getNgayBatDau() == null
                                ? null
                                : entity.getNgayBatDau().toLocalDate()
                )

                .endDate(
                        entity.getNgayKetThuc() == null
                                ? null
                                : entity.getNgayKetThuc().toLocalDate()
                )

                .status(entity.getTrangThai())

                .statusLabel(
                        getStatusLabel(entity.getTrangThai())
                )

                .timeStatus(
                        getTimeStatus(entity)
                )

                .description(entity.getMoTa())

                .productDetailIds(productDetailIds)

                .build();
    }
    // =====================================================
    // LABEL TRẠNG THÁI
    // =====================================================
    @Transactional
    public DotGiamGiaResponse updateStatus(
            String ma,
            Integer status
    ) {

        // Chỉ cho phép trạng thái 0 hoặc 1
        if (status == null || (status != 0 && status != 1)) {
            throw AppException.badRequest(
                    "Trạng thái chỉ được phép là 0 hoặc 1"
            );
        }

        // Tìm đợt giảm giá
        DotGiamGia entity = dotGiamGiaRepository
                .findByMaDotGiamGia(ma)
                .orElseThrow(() ->
                        AppException.notFound(
                                "Không tìm thấy đợt giảm giá: " + ma
                        )
                );
        // Không cho thay đổi trạng thái đợt đã xóa
        if (Integer.valueOf(-1).equals(entity.getTrangThai())) {
            throw AppException.notFound(
                    "Không tìm thấy đợt giảm giá: " + ma
            );
        }
        // Lấy toàn bộ biến thể thuộc đợt giảm giá
        List<ChiTietDotGiamGia> chiTietList =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGia_Id(entity.getId());

        // ==========================================
        // NẾU KÍCH HOẠT LẠI -> KIỂM TRA TRÙNG
        // ==========================================
        if (status == 1) {

            List<Long> productDetailIds =
                    chiTietList.stream()
                            .map(chiTiet ->
                                    chiTiet
                                            .getIdSanPhamChiTiet()
                                            .getId()
                            )
                            .distinct()
                            .toList();

            LocalDate startDate =
                    entity
                            .getNgayBatDau()
                            .toLocalDate();

            LocalDate endDate =
                    entity
                            .getNgayKetThuc()
                            .toLocalDate();

            // Kiểm tra xem các biến thể có đang nằm
            // trong đợt giảm giá khác bị trùng ngày không
            validateProductPromotionConflict(
                    productDetailIds,
                    startDate,
                    endDate,
                    entity.getId()
            );
        }

        // ==========================================
        // CẬP NHẬT TRẠNG THÁI ĐỢT GIẢM GIÁ
        // ==========================================

        entity.setTrangThai(status);
        entity.setKichHoat(status == 1);
        entity.setNgayCapNhat(LocalDateTime.now());

        DotGiamGia saved =
                dotGiamGiaRepository.save(entity);

        // ==========================================
        // ĐỒNG BỘ TRẠNG THÁI CÁC BIẾN THỂ
        // ==========================================

        for (ChiTietDotGiamGia chiTiet : chiTietList) {
            chiTiet.setTrangThai(status);
        }

        chiTietDotGiamGiaRepository.saveAll(
                chiTietList
        );

        return toResponse(saved);
    }

    private String getTimeStatus(DotGiamGia entity) {

        if (entity.getTrangThai() == null
                || entity.getTrangThai() == 0) {
            return "NGUNG_HOAT_DONG";
        }

        LocalDate today = LocalDate.now();

        LocalDate startDate =
                entity.getNgayBatDau().toLocalDate();

        LocalDate endDate =
                entity.getNgayKetThuc().toLocalDate();

        if (today.isBefore(startDate)) {
            return "SAP_DIEN_RA";
        }

        if (today.isAfter(endDate)) {
            return "DA_KET_THUC";
        }

        return "DANG_DIEN_RA";
    }
    private String getStatusLabel(
            Integer status
    ) {

        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {

            case 0 ->
                    "Ngừng hoạt động";

            case 1 ->
                    "Đang hoạt động";

            default ->
                    "Không xác định";
        };
    }
}