package com.smashstep.datn.promotion.service;

import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.promotion.dto.DotGiamGiaRequest;
import com.smashstep.datn.promotion.dto.DotGiamGiaResponse;
import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import com.smashstep.datn.promotion.entity.DotGiamGia;
import com.smashstep.datn.promotion.repository.ChiTietDotGiamGiaRepository;
import com.smashstep.datn.promotion.repository.DotGiamGiaRepository;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.product.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smashstep.datn.promotion.dto.ChiTietDotGiamGiaResponse;
import com.smashstep.datn.promotion.dto.DotGiamGiaDetailResponse;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DotGiamGiaService {

    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @Transactional
    public DotGiamGia create(DotGiamGiaRequest request) {

        DotGiamGia dotGiamGia = new DotGiamGia();

        dotGiamGia.setMaDotGiamGia(request.getMaDotGiamGia());
        dotGiamGia.setTenDotGiamGia(request.getTenDotGiamGia());
        dotGiamGia.setPhanTramGiamDot(request.getPhanTramGiamDot());
        dotGiamGia.setNgayBatDau(request.getNgayBatDau());
        dotGiamGia.setNgayKetThuc(request.getNgayKetThuc());
        dotGiamGia.setKichHoat(
                request.getKichHoat() != null
                        ? request.getKichHoat()
                        : true
        );
        dotGiamGia.setMoTa(request.getMoTa());
        dotGiamGia.setNgayTao(LocalDateTime.now());
        dotGiamGia.setNgayCapNhat(LocalDateTime.now());
        dotGiamGia.setTrangThai(1);

        DotGiamGia saved = dotGiamGiaRepository.save(dotGiamGia);

        if (request.getChiTiet() != null) {
            for (var item : request.getChiTiet()) {

                SanPhamChiTiet sanPhamChiTiet =
                        sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet())
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Không tìm thấy biến thể sản phẩm: "
                                                        + item.getIdSanPhamChiTiet()
                                        )
                                );

                ChiTietDotGiamGia chiTiet = new ChiTietDotGiamGia();

                chiTiet.setIdDotGiamGia(saved);
                chiTiet.setIdSanPhamChiTiet(sanPhamChiTiet);
                chiTiet.setPhanTramGiamBienThe(
                        item.getPhanTramGiamBienThe()
                );
                chiTiet.setTrangThai(1);
                chiTiet.setNgayTao(LocalDateTime.now());

                chiTietDotGiamGiaRepository.save(chiTiet);
            }
        }

        return saved;
    }
    @Transactional
    public void delete(Long id) {

        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy đợt giảm giá: " + id
                        )
                );

        // Xóa mềm đợt giảm giá
        dotGiamGia.setTrangThai(0);
        dotGiamGia.setKichHoat(false);
        dotGiamGia.setNgayCapNhat(LocalDateTime.now());

        dotGiamGiaRepository.save(dotGiamGia);

        // Xóa mềm các biến thể thuộc đợt giảm giá
        var chiTietList =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGiaIdAndTrangThai(id, 1);

        for (ChiTietDotGiamGia chiTiet : chiTietList) {
            chiTiet.setTrangThai(0);
        }

        chiTietDotGiamGiaRepository.saveAll(chiTietList);
    }
    @Transactional
    public DotGiamGia update(Long id, DotGiamGiaRequest request) {

        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy đợt giảm giá: " + id
                        )
                );

        dotGiamGia.setMaDotGiamGia(request.getMaDotGiamGia());
        dotGiamGia.setTenDotGiamGia(request.getTenDotGiamGia());
        dotGiamGia.setPhanTramGiamDot(request.getPhanTramGiamDot());
        dotGiamGia.setNgayBatDau(request.getNgayBatDau());
        dotGiamGia.setNgayKetThuc(request.getNgayKetThuc());
        dotGiamGia.setKichHoat(
                request.getKichHoat() != null
                        ? request.getKichHoat()
                        : dotGiamGia.getKichHoat()
        );
        dotGiamGia.setMoTa(request.getMoTa());
        dotGiamGia.setNgayCapNhat(LocalDateTime.now());

        // Tắt các chi tiết cũ
        var chiTietCu =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGiaIdAndTrangThai(id, 1);

        for (ChiTietDotGiamGia chiTiet : chiTietCu) {
            chiTiet.setTrangThai(0);
        }

        chiTietDotGiamGiaRepository.saveAll(chiTietCu);

        // Thêm lại danh sách chi tiết mới
        if (request.getChiTiet() != null) {

            for (var item : request.getChiTiet()) {

                SanPhamChiTiet sanPhamChiTiet =
                        sanPhamChiTietRepository.findById(
                                item.getIdSanPhamChiTiet()
                        ).orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy biến thể sản phẩm: "
                                                + item.getIdSanPhamChiTiet()
                                )
                        );

                ChiTietDotGiamGia chiTiet =
                        new ChiTietDotGiamGia();

                chiTiet.setIdDotGiamGia(dotGiamGia);
                chiTiet.setIdSanPhamChiTiet(sanPhamChiTiet);
                chiTiet.setPhanTramGiamBienThe(
                        item.getPhanTramGiamBienThe()
                );
                chiTiet.setTrangThai(1);
                chiTiet.setNgayTao(LocalDateTime.now());

                chiTietDotGiamGiaRepository.save(chiTiet);
            }
        }

        return dotGiamGiaRepository.save(dotGiamGia);
    }
    public PageResponse<DotGiamGiaResponse> search(
            String ma,
            String ten,
            Integer trangThai,
            LocalDateTime tuNgay,
            LocalDateTime denNgay,
            int page,
            int size
    ) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<DotGiamGia> result = dotGiamGiaRepository.search(
                ma,
                ten,
                trangThai,
                tuNgay,
                denNgay,
                pageable
        );

        Page<DotGiamGiaResponse> responsePage =
                result.map(this::toResponse);

        return PageResponse.from(responsePage);
    }
    @Transactional(readOnly = true)
    public DotGiamGiaDetailResponse getDetail(Long id) {

        DotGiamGia dotGiamGia = dotGiamGiaRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy đợt giảm giá: " + id
                        )
                );

        var chiTietList =
                chiTietDotGiamGiaRepository
                        .findByIdDotGiamGiaIdAndTrangThai(id, 1)
                        .stream()
                        .map(this::toChiTietResponse)
                        .toList();

        String trangThaiText;

        if (dotGiamGia.getTrangThai() != null
                && dotGiamGia.getTrangThai() == 1) {
            trangThaiText = "Đang hoạt động";
        } else {
            trangThaiText = "Ngừng hoạt động";
        }

        return new DotGiamGiaDetailResponse(
                dotGiamGia.getId(),
                dotGiamGia.getMaDotGiamGia(),
                dotGiamGia.getTenDotGiamGia(),
                dotGiamGia.getPhanTramGiamDot(),
                dotGiamGia.getNgayBatDau(),
                dotGiamGia.getNgayKetThuc(),
                dotGiamGia.getKichHoat(),
                dotGiamGia.getTrangThai(),
                trangThaiText,
                dotGiamGia.getMoTa(),
                dotGiamGia.getNgayTao(),
                dotGiamGia.getNgayCapNhat(),
                chiTietList
        );
    }

    private ChiTietDotGiamGiaResponse toChiTietResponse(
            ChiTietDotGiamGia entity
    ) {
        SanPhamChiTiet sanPhamChiTiet = entity.getIdSanPhamChiTiet();

        return new ChiTietDotGiamGiaResponse(
                entity.getId(),
                sanPhamChiTiet.getId(),
                sanPhamChiTiet.getMaChiTietSanPham(),
                sanPhamChiTiet.getSku(),
                sanPhamChiTiet.getGiaBan(),
                entity.getPhanTramGiamBienThe(),
                entity.getTrangThai()
        );
    }

    private DotGiamGiaResponse toResponse(DotGiamGia entity) {

        String trangThaiText;

        if (entity.getTrangThai() != null && entity.getTrangThai() == 1) {
            trangThaiText = "Đang hoạt động";
        } else {
            trangThaiText = "Ngừng hoạt động";
        }

        return new DotGiamGiaResponse(
                entity.getId(),
                entity.getMaDotGiamGia(),
                entity.getTenDotGiamGia(),
                entity.getPhanTramGiamDot(),
                entity.getNgayBatDau(),
                entity.getNgayKetThuc(),
                entity.getKichHoat(),
                entity.getTrangThai(),
                trangThaiText,
                entity.getMoTa(),
                entity.getNgayTao(),
                entity.getNgayCapNhat()
        );
    }
}