package com.smashstep.datn.employee.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
@Entity @Table(name="employee_import_mail",uniqueConstraints=@UniqueConstraint(columnNames={"employee_id"})) @Getter @Setter
public class NhanVienImportMail {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="batch_id",nullable=false) private NhanVienImportBatch batch;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="employee_id",nullable=false) private NhanVien employee;
    @Column(name="row_number",nullable=false) private Integer rowNumber;
    @Column(nullable=false,length=12) private String state;
    @JsonIgnore @Column(name="secret_cipher",length=1024) private String secretCipher;
    @Column(nullable=false) private Integer attempts;
    @org.hibernate.annotations.Nationalized @Column(name="last_error",length=300) private String lastError;
    @Column(name="sent_at") private LocalDateTime sentAt;
}
