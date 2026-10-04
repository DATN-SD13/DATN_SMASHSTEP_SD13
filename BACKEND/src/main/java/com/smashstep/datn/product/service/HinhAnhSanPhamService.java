package com.smashstep.datn.product.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.product.dto.HinhAnhSanPhamRequest;
import com.smashstep.datn.product.config.ThuMucAnhSanPham;
import com.smashstep.datn.product.dto.HinhAnhSanPhamResponse;
import com.smashstep.datn.product.entity.HinhAnhSanPham;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.repository.HinhAnhSanPhamRepository;
import com.smashstep.datn.product.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HinhAnhSanPhamService {
    private final SanPhamRepository sanPhamRepository;
    private final HinhAnhSanPhamRepository hinhAnhRepository;
    @Value("${smashstep.product-images.directory:uploads/products}")
    private String thuMucAnh = "uploads/products";
    private static final long DUNG_LUONG_TOI_DA = 5L * 1024 * 1024;
    private static final String URL_ANH = "/uploads/products/";

    @Transactional
    public HinhAnhSanPhamResponse themAnh(Long sanPhamId, HinhAnhSanPhamRequest yeuCau) {
        SanPham sanPham = sanPhamRepository.timVaKhoaTheoId(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        String urlAnh = kiemTraUrlAnh(yeuCau.getUrlAnh());
        return luuAnh(sanPham, urlAnh, yeuCau.getIsAnhChinh());
    }

    @Transactional
    public HinhAnhSanPhamResponse taiAnh(Long sanPhamId, MultipartFile file, Boolean isAnhChinh, Long mauSacId) {
        SanPham sanPham = sanPhamRepository.timVaKhoaTheoId(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        if (mauSacId != null) {
            throw AppException.badRequest("Database hiện tại không hỗ trợ ảnh theo màu; hãy bỏ mauSacId");
        }
        if (file == null || file.isEmpty()) throw AppException.badRequest("Tệp ảnh không được để trống.");
        if (file.getSize() > DUNG_LUONG_TOI_DA) throw AppException.badRequest("Ảnh không được vượt quá 5 MB.");
        String mime = Objects.toString(file.getContentType(), "").toLowerCase(Locale.ROOT);
        String phanMoRong = switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw AppException.badRequest("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.");
        };
        Path tep = null;
        try {
            byte[] noiDung = file.getBytes();
            if (noiDung.length > DUNG_LUONG_TOI_DA) throw AppException.badRequest("Ảnh không được vượt quá 5 MB.");
            if (!dungDinhDang(noiDung, phanMoRong)) throw AppException.badRequest("Tệp ảnh không hợp lệ.");
            Path thuMuc = ThuMucAnhSanPham.resolve(thuMucAnh);
            Files.createDirectories(thuMuc);
            String tenTep = UUID.randomUUID() + "." + phanMoRong;
            tep = thuMuc.resolve(tenTep);
            Files.write(tep, noiDung, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Path tepMoi = tep;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) xoaTep(tepMoi);
                    }
                });
            }
            return luuAnh(sanPham, URL_ANH + tenTep, isAnhChinh);
        } catch (IOException loi) {
            if (tep != null) xoaTep(tep);
            throw new AppException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp ảnh.");
        } catch (RuntimeException loi) {
            if (tep != null) xoaTep(tep);
            throw loi;
        }
    }

    private boolean dungDinhDang(byte[] noiDung, String loai) {
        if (noiDung.length < 12) return false;
        return switch (loai) {
            case "jpg" -> (noiDung[0] & 255) == 255 && (noiDung[1] & 255) == 216 && (noiDung[2] & 255) == 255;
            case "png" -> Arrays.equals(Arrays.copyOf(noiDung, 8), new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
            case "webp" -> noiDung[0] == 'R' && noiDung[1] == 'I' && noiDung[2] == 'F' && noiDung[3] == 'F'
                    && noiDung[8] == 'W' && noiDung[9] == 'E' && noiDung[10] == 'B' && noiDung[11] == 'P';
            default -> false;
        };
    }

    private HinhAnhSanPhamResponse luuAnh(SanPham sanPham, String urlAnh, Boolean isAnhChinh) {
        List<HinhAnhSanPham> danhSach = hinhAnhRepository.findByIdSanPham_IdOrderByIdAsc(sanPham.getId());
        boolean laAnhChinh = Boolean.TRUE.equals(isAnhChinh) || danhSach.isEmpty();
        if (laAnhChinh) {
            for (HinhAnhSanPham anh : danhSach) {
                anh.setIsAnhChinh(false);
            }
        }
        HinhAnhSanPham anh = new HinhAnhSanPham();
        anh.setIdSanPham(sanPham);
        anh.setUrlAnh(urlAnh);
        anh.setIsAnhChinh(laAnhChinh);
        return chuyenSangResponse(hinhAnhRepository.save(anh));
    }

    public List<HinhAnhSanPhamResponse> layDanhSachAnh(Long sanPhamId) {
        sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPhamResponse> ketQua = new ArrayList<>();
        for (HinhAnhSanPham anh : hinhAnhRepository.findByIdSanPham_IdOrderByIdAsc(sanPhamId)) {
            ketQua.add(chuyenSangResponse(anh));
        }
        return ketQua;
    }

    @Transactional
    public HinhAnhSanPhamResponse suaAnhTheoId(Long anhId, HinhAnhSanPhamRequest yeuCau) {
        Long sanPhamId = hinhAnhRepository.laySanPhamIdTheoAnhId(anhId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ảnh sản phẩm"));
        return suaAnh(sanPhamId, anhId, yeuCau);
    }

    @Transactional
    public void xoaAnhTheoId(Long anhId) {
        Long sanPhamId = hinhAnhRepository.laySanPhamIdTheoAnhId(anhId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ảnh sản phẩm"));
        xoaAnh(sanPhamId, anhId);
    }

    @Transactional
    public HinhAnhSanPhamResponse suaAnh(Long sanPhamId, Long anhId, HinhAnhSanPhamRequest yeuCau) {
        sanPhamRepository.timVaKhoaTheoId(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> danhSach = hinhAnhRepository.findByIdSanPham_IdOrderByIdAsc(sanPhamId);
        HinhAnhSanPham anhCanSua = timAnh(danhSach, anhId);
        String urlCu = anhCanSua.getUrlAnh();
        anhCanSua.setUrlAnh(kiemTraUrlAnh(yeuCau.getUrlAnh()));
        // Đổi ảnh chính bằng cách chọn ảnh khác, tránh mất ảnh chính khi gửi false.
        boolean laAnhChinh = Boolean.TRUE.equals(yeuCau.getIsAnhChinh())
                || Boolean.TRUE.equals(anhCanSua.getIsAnhChinh());
        if (laAnhChinh) {
            for (HinhAnhSanPham anh : danhSach) {
                anh.setIsAnhChinh(anh.getId().equals(anhId));
            }
        }
        HinhAnhSanPhamResponse ketQua = chuyenSangResponse(hinhAnhRepository.save(anhCanSua));
        if (!Objects.equals(urlCu, anhCanSua.getUrlAnh())) donTepSauKhiLuu(urlCu);
        return ketQua;
    }

    @Transactional
    public void xoaAnh(Long sanPhamId, Long anhId) {
        sanPhamRepository.timVaKhoaTheoId(sanPhamId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> danhSach = hinhAnhRepository.findByIdSanPham_IdOrderByIdAsc(sanPhamId);
        HinhAnhSanPham anhCanXoa = timAnh(danhSach, anhId);
        hinhAnhRepository.delete(anhCanXoa);
        donTepSauKhiLuu(anhCanXoa.getUrlAnh());
        if (Boolean.TRUE.equals(anhCanXoa.getIsAnhChinh())) {
            for (HinhAnhSanPham anh : danhSach) {
                if (!anh.getId().equals(anhId)) {
                    anh.setIsAnhChinh(true);
                    break;
                }
            }
        }
    }

    private HinhAnhSanPham timAnh(List<HinhAnhSanPham> danhSach, Long anhId) {
        for (HinhAnhSanPham anh : danhSach) {
            if (anh.getId().equals(anhId)) {
                return anh;
            }
        }
        throw AppException.notFound("Không tìm thấy ảnh của sản phẩm");
    }

    private String kiemTraUrlAnh(String giaTri) {
        String urlAnh = QuyTacSanPham.boKhoangTrang(giaTri);
        Path tep = tepDaTai(urlAnh);
        if (tep != null) {
            if (!Files.isRegularFile(tep)) throw AppException.badRequest("Không tìm thấy tệp ảnh đã tải lên.");
            return urlAnh;
        }
        try {
            URI diaChi = URI.create(urlAnh);
            String giaoThuc = Objects.toString(diaChi.getScheme(), "").toLowerCase(Locale.ROOT);
            if (!Set.of("http", "https").contains(giaoThuc)
                    || diaChi.getHost() == null || diaChi.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException loi) {
            throw AppException.badRequest("URL ảnh phải là địa chỉ HTTP/HTTPS hợp lệ");
        }
        return urlAnh;
    }

    private Path tepDaTai(String urlAnh) {
        if (urlAnh == null || !urlAnh.matches("^/uploads/products/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$")) return null;
        return ThuMucAnhSanPham.resolve(thuMucAnh).resolve(urlAnh.substring(URL_ANH.length()));
    }

    private void donTepSauKhiLuu(String urlAnh) {
        Path tep = tepDaTai(urlAnh);
        if (tep == null) return;
        Runnable donTep = () -> {
            if (!hinhAnhRepository.existsByUrlAnh(urlAnh)) xoaTep(tep);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { donTep.run(); }
            });
        } else donTep.run();
    }

    private void xoaTep(Path tep) {
        try { Files.deleteIfExists(tep); }
        catch (IOException loi) { org.slf4j.LoggerFactory.getLogger(HinhAnhSanPhamService.class).warn("Không thể dọn tệp ảnh {}", tep.getFileName(), loi); }
    }

    static HinhAnhSanPhamResponse chuyenSangResponse(HinhAnhSanPham anh) {
        return new HinhAnhSanPhamResponse(anh.getId(), anh.getUrlAnh(), anh.getIsAnhChinh(), anh.getIdSanPham().getId());
    }

    static Map<Long, String> layAnhChinh(HinhAnhSanPhamRepository hinhAnhRepository, List<Long> danhSachId) {
        Map<Long, String> ketQua = new LinkedHashMap<>();
        if (!danhSachId.isEmpty()) {
            for (HinhAnhSanPham anh : hinhAnhRepository.findByIdSanPham_IdInAndIsAnhChinhTrueOrderByIdAsc(danhSachId)) {
                ketQua.putIfAbsent(anh.getIdSanPham().getId(), anh.getUrlAnh());
            }
        }
        return ketQua;
    }
}
