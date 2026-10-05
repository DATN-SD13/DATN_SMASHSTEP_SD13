package com.smashstep.datn.product.controller;

import com.smashstep.datn.product.dto.HinhAnhSanPhamRequest;
import com.smashstep.datn.product.dto.HinhAnhSanPhamResponse;
import com.smashstep.datn.product.service.HinhAnhSanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RequiredArgsConstructor
public class HinhAnhSanPhamController {
    private final HinhAnhSanPhamService hinhAnhService;

    @GetMapping("/products/{productId}/images")
    public List<HinhAnhSanPhamResponse> layDanhSach(@PathVariable("productId") Long sanPhamId) {
        return hinhAnhService.layDanhSachAnh(sanPhamId);
    }

    @PostMapping("/products/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public HinhAnhSanPhamResponse themAnh(@PathVariable("productId") Long sanPhamId,
            @Valid @RequestBody HinhAnhSanPhamRequest yeuCau) {
        return hinhAnhService.themAnh(sanPhamId, yeuCau);
    }

    @PostMapping(value = "/products/{productId}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public HinhAnhSanPhamResponse taiAnh(@PathVariable("productId") Long sanPhamId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isAnhChinh", defaultValue = "false") Boolean isAnhChinh,
            @RequestParam(value = "mauSacId", required = false) Long mauSacId) {
        return hinhAnhService.taiAnh(sanPhamId, file, isAnhChinh, mauSacId);
    }

    @PutMapping("/product-images/{id}")
    public HinhAnhSanPhamResponse suaAnhTheoId(@PathVariable("id") Long anhId,
            @Valid @RequestBody HinhAnhSanPhamRequest yeuCau) {
        return hinhAnhService.suaAnhTheoId(anhId, yeuCau);
    }

    @DeleteMapping("/product-images/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void xoaAnhTheoId(@PathVariable("id") Long anhId) {
        hinhAnhService.xoaAnhTheoId(anhId);
    }

    @PutMapping("/products/{productId}/images/{imageId}")
    public HinhAnhSanPhamResponse suaAnh(@PathVariable("productId") Long sanPhamId,
            @PathVariable("imageId") Long anhId, @Valid @RequestBody HinhAnhSanPhamRequest yeuCau) {
        return hinhAnhService.suaAnh(sanPhamId, anhId, yeuCau);
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void xoaAnh(@PathVariable("productId") Long sanPhamId, @PathVariable("imageId") Long anhId) {
        hinhAnhService.xoaAnh(sanPhamId, anhId);
    }
}
