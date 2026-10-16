package com.smashstep.datn.support;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import jakarta.persistence.EntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

/** Test identity matching the JWT filter contract; no account/password/database changes. */
public final class EmployeeSecurityFixture {
    private EmployeeSecurityFixture() {}
    public static void authenticate(EntityManager em) {
        Long employeeId = em.createQuery("select n.id from NhanVien n where n.trangThai=1 order by n.id",Long.class)
                .setMaxResults(1).getSingleResult();
        var auth = new UsernamePasswordAuthenticationToken("integration-employee",null,List.of(new SimpleGrantedAuthority("ROLE_STAFF")));
        auth.setDetails(new AuthDetails(employeeId,"STAFF","NHAN_VIEN"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    public static void clear() { SecurityContextHolder.clearContext(); }
}
