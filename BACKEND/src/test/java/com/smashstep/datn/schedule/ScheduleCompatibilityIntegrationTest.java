package com.smashstep.datn.schedule;

import com.smashstep.datn.DatnApplication;
import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.schedule.service.ScheduleAvailability;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
@Transactional(readOnly=true)
class ScheduleCompatibilityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ScheduleAvailability availability;
    MockMvc mvc;
    void role(String role){var auth=new UsernamePasswordAuthenticationToken("integration",null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));auth.setDetails(new AuthDetails(1L,role,"ADMIN".equals(role)?"NHAN_VIEN":"KHACH_HANG"));SecurityContextHolder.getContext().setAuthentication(auth);}
    @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).build();role("ADMIN");}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void capabilitiesDescribeRealStorageWithoutChangingSchema() throws Exception {
        mvc.perform(get("/api/lich-lam-viec/capabilities")).andExpect(status().isOk()).andExpect(jsonPath("$.data.available").value(availability.available()));
    }
    @Test void unavailableStorageHasFriendly503InsteadOfInvalidObject500() throws Exception {
        mvc.perform(get("/api/ca-lam-viec")).andExpect(status().is(availability.available()?200:503));
        mvc.perform(get("/api/lich-lam-viec")).andExpect(status().is(availability.available()?200:503));
    }
    @Test void customersCannotAccessEmployeeSchedules() throws Exception {
        role("CUSTOMER");mvc.perform(get("/api/lich-lam-viec/capabilities")).andExpect(status().isForbidden());
    }
    @Test void absentIdentityIsUnauthorizedRatherThanInternalError() throws Exception {
        SecurityContextHolder.clearContext();mvc.perform(get("/api/lich-lam-viec/capabilities")).andExpect(status().isUnauthorized());
    }
}
