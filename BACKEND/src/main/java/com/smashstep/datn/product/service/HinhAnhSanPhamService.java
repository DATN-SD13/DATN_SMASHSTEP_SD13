package com.smashstep.datn.product.service;

import com.smashstep.datn.product.dto.ImageRequest;
import com.smashstep.datn.product.dto.ImageResponse;
import com.smashstep.datn.product.entity.HinhAnhSanPham;
import com.smashstep.datn.product.entity.SanPham;
import com.smashstep.datn.product.repository.HinhAnhSanPhamRepository;
import com.smashstep.datn.product.repository.SanPhamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HinhAnhSanPhamService {
    private final SanPhamRepository products;
    private final HinhAnhSanPhamRepository images;

    @Transactional
    public ImageResponse addImage(Long productId, ImageRequest request) {
        SanPham p = products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        String url = imageUrl(request.getUrlAnh());
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        boolean main = request.getIsAnhChinh() || current.isEmpty();
        if (main) current.forEach(image -> image.setIsAnhChinh(false));
        HinhAnhSanPham image = new HinhAnhSanPham();
        image.setIdSanPham(p); image.setUrlAnh(url); image.setIsAnhChinh(main);
        return toResponse(images.save(image));
    }

    public List<ImageResponse> listImages(Long productId) {
        find(productId);
        return images.findByIdSanPham_IdOrderByIdAsc(productId).stream().map(HinhAnhSanPhamService::toResponse).toList();
    }

    @Transactional
    public ImageResponse updateImageById(Long imageId, ImageRequest request) {
        Long productId = images.findProductIdById(imageId)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh sản phẩm"));
        return updateImage(productId, imageId, request);
    }

    @Transactional
    public void removeImageById(Long imageId) {
        Long productId = images.findProductIdById(imageId)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh sản phẩm"));
        removeImage(productId, imageId);
    }

    @Transactional
    public ImageResponse updateImage(Long productId, Long imageId, ImageRequest request) {
        products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        HinhAnhSanPham image = current.stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh của sản phẩm"));
        image.setUrlAnh(imageUrl(request.getUrlAnh()));
        // Keep one main image when editing; select another image to replace the main one.
        boolean main = request.getIsAnhChinh() || Boolean.TRUE.equals(image.getIsAnhChinh());
        if (main) current.forEach(i -> i.setIsAnhChinh(i.getId().equals(imageId)));
        return toResponse(images.save(image));
    }

    @Transactional
    public void removeImage(Long productId, Long imageId) {
        products.findLockedById(productId).orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
        List<HinhAnhSanPham> current = images.findByIdSanPham_IdOrderByIdAsc(productId);
        HinhAnhSanPham target = current.stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy ảnh của sản phẩm"));
        images.delete(target);
        if (Boolean.TRUE.equals(target.getIsAnhChinh()))
            current.stream().filter(i -> !i.getId().equals(imageId)).findFirst().ifPresent(i -> i.setIsAnhChinh(true));
    }

    private String imageUrl(String input) {
        String url = ProductRules.trim(input);
        try {
            URI uri = URI.create(url);
            if (!Set.of("http", "https").contains(Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw ProductException.badRequest("URL ảnh phải là địa chỉ HTTP/HTTPS hợp lệ"); }
        return url;
    }

    static ImageResponse toResponse(HinhAnhSanPham image) {
        return new ImageResponse(image.getId(), image.getUrlAnh(), image.getIsAnhChinh());
    }

    private SanPham find(Long productId) {
        return products.findById(productId)
                .orElseThrow(() -> ProductException.notFound("Không tìm thấy sản phẩm"));
    }

    static Map<Long, String> mainImageUrls(HinhAnhSanPhamRepository images, List<Long> ids) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (!ids.isEmpty()) images.findByIdSanPham_IdInAndIsAnhChinhTrueOrderByIdAsc(ids)
                .forEach(image -> result.putIfAbsent(image.getIdSanPham().getId(), image.getUrlAnh()));
        return result;
    }
}
