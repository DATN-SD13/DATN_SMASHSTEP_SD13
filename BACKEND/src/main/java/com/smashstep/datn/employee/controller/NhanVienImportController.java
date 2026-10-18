package com.smashstep.datn.employee.controller;
import com.smashstep.datn.employee.service.NhanVienImportService;
import com.smashstep.datn.employee.dto.NhanVienImportDto;
import com.smashstep.datn.employee.exception.NhanVienImportException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import static com.smashstep.datn.employee.dto.NhanVienImportDto.*;
@RestController @RequestMapping("/api/nhan-vien/excel") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class NhanVienImportController {
    private final NhanVienImportService service;
    @GetMapping("/mau") public ResponseEntity<byte[]> template(){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=nhan-vien-mau.xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(service.template());}
    @PostMapping(value="/xem-truoc",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<Preview> preview(@RequestParam("file") MultipartFile file){return ApiResponse.ok(service.preview(file));}
    @PostMapping("/xac-nhan") public ResponseEntity<?> confirm(@Valid @RequestBody Confirm request){try{return ResponseEntity.ok(ApiResponse.ok(service.confirm(request)));}catch(NhanVienImportException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage(),e.getPreview()));}}
    @GetMapping public ApiResponse<List<Result>> recent(){return ApiResponse.ok(service.recent());}
    @GetMapping("/{id}") public ApiResponse<Result> result(@PathVariable String id){return ApiResponse.ok(service.result(id));}
    @PostMapping("/{id}/gui-lai-email") public ApiResponse<Result> retry(@PathVariable String id){return ApiResponse.ok(service.retry(id));}
}
