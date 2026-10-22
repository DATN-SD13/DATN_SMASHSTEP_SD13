package com.smashstep.datn.product.controller;

import com.smashstep.datn.common.response.ApiResponse;
import com.smashstep.datn.product.dto.SanPhamImportDto.*;
import com.smashstep.datn.product.exception.SanPhamImportException;
import com.smashstep.datn.product.service.SanPhamImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/products/excel") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class SanPhamImportController {
    private final SanPhamImportService service;
    @GetMapping("/mau") public ResponseEntity<byte[]> mau(){return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=san-pham-mau.xlsx").body(service.mau());}
    @PostMapping(value="/xem-truoc",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<XemTruoc> xemTruoc(@RequestParam("file") MultipartFile file){return ApiResponse.ok(service.xemTruoc(file));}
    @PostMapping("/xac-nhan") public ResponseEntity<?> xacNhan(@Valid @RequestBody XacNhan request){try{return ResponseEntity.ok(ApiResponse.ok(service.xacNhan(request)));}catch(SanPhamImportException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage(),e.getXemTruoc()));}}
    @GetMapping("/{id}") public ApiResponse<KetQua> ketQua(@PathVariable String id){return ApiResponse.ok(service.ketQua(id));}
}
