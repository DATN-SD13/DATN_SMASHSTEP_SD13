package com.smashstep.datn.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="san_pham_import_batch") @Getter @Setter
public class SanPhamImportBatch {
    @Id @Column(length=36) private String id;
    @Column(name="nguoi_tao",nullable=false) private Long nguoiTao;
    @Column(name="payload_hash",nullable=false,length=64) private String payloadHash;
    @Column(name="ngay_tao",nullable=false) private LocalDateTime ngayTao;
    @Column(name="ket_qua_json",nullable=false,columnDefinition="NVARCHAR(MAX)") private String ketQuaJson;
}
