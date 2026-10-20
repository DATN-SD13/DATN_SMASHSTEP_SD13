package com.smashstep.datn.employee.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
public final class NhanVienImportDto {
    private NhanVienImportDto() {}
    public record Row(@Min(2) @Max(501) int rowNumber,@NotNull @Valid NhanVienRequest employee) {}
    public record CheckedRow(int rowNumber,NhanVienRequest employee,List<String> errors) {}
    public record Preview(String requestId,int totalRows,long validRows,long invalidRows,List<CheckedRow> rows) {}
    public record Confirm(@NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId,@NotEmpty @Size(max=500) List<@NotNull @Valid Row> rows) {}
    public record MailResult(Long mailId,int rowNumber,String employeeCode,String name,String email,String state,int attempts,String error) {}
    public record Result(String batchId,LocalDateTime createdAt,int createdCount,long emailSent,long emailFailed,long emailPending,List<MailResult> rows) {}
}
