package com.smashstep.datn.sales.service;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Read only the identity established by the existing JWT filter. */
public final class PosActor {
    private PosActor() {}
    public static Long employeeId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw AppException.unauthorized("Chưa đăng nhập");
        if (!(auth.getDetails() instanceof AuthDetails d)
                || !"NHAN_VIEN".equals(d.userType()) || d.userId() == null
                || !("ADMIN".equals(d.role()) || "STAFF".equals(d.role()))
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + d.role())))
            throw AppException.forbidden("Chỉ nhân viên được sử dụng POS");
        return d.userId();
    }
}
