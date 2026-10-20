package com.smashstep.datn.employee.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="employee_import_batch") @Getter @Setter
public class NhanVienImportBatch {
    @Id @Column(length=36) private String id;
    @Column(name="payload_hash",nullable=false,length=64) private String payloadHash;
    @Column(name="created_by",nullable=false) private Long createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
