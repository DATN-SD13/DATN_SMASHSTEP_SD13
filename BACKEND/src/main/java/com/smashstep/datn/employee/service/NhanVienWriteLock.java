package com.smashstep.datn.employee.service;
import com.smashstep.datn.common.exception.AppException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class NhanVienWriteLock {
    private final EntityManager em;
    public void lock() {
        int result=em.unwrap(Session.class).doReturningWork(c -> {
            if(c.getAutoCommit()) throw new java.sql.SQLException("Employee writes require a transaction");
            try(var s=c.prepareStatement("SELECT TOP (1) object_id FROM sys.objects");var ignored=s.executeQuery()) { }
            try(var s=c.prepareStatement("DECLARE @r int; EXEC @r=sys.sp_getapplock @Resource=N'smashstep:employee:write', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=15000; SELECT @r");var r=s.executeQuery()) { r.next();return r.getInt(1); }
        });
        if(result<0)throw AppException.conflict("Đang xử lý nhân viên khác. Vui lòng thử lại.");
    }
}
