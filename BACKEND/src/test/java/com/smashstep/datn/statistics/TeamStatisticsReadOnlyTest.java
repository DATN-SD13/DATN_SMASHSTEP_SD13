package com.smashstep.datn.statistics;

import com.smashstep.datn.DatnApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=DatnApplication.class,properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false","spring.jpa.open-in-view=false"})
@Transactional(readOnly=true)
class TeamStatisticsReadOnlyTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).build();}
    @ParameterizedTest @ValueSource(strings={"tong-quan","bieu-do","nhan-vien","san-pham","chu-ky"})
    void newTeamStatisticsEndpointsReadCanonicalTables(String endpoint) throws Exception {
        mvc.perform(get("/api/thong-ke-tong-hop/"+endpoint).param("tuNgay","2026-01-01").param("denNgay","2026-12-31"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").exists());
    }
    @ParameterizedTest @ValueSource(strings={"tong-quan","bieu-do","nhan-vien","san-pham"})
    void reversedDatesReturn400InsteadOf500(String endpoint) throws Exception {
        mvc.perform(get("/api/thong-ke-tong-hop/"+endpoint).param("tuNgay","2026-12-31").param("denNgay","2026-01-01"))
            .andExpect(status().isBadRequest());
    }
}
