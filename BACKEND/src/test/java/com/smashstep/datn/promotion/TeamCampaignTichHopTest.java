package com.smashstep.datn.promotion;

import com.smashstep.datn.DatnApplication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DatnApplication.class, properties = {"spring.jpa.show-sql=false", "spring.jpa.hibernate.ddl-auto=none"})
@Transactional
class TeamCampaignTichHopTest {
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    final JsonMapper mapper = JsonMapper.builder().build();
    Map<String,Object> request() {
        Long variant = em.createQuery("select s.id from SanPhamChiTiet s where s.trangThai=1 and s.kichHoat=true and s.idSanPham.trangThai=1 order by s.id",Long.class).setMaxResults(1).getSingleResult();
        return new LinkedHashMap<>(Map.of("maDotGiamGia", "SYNC-"+UUID.randomUUID().toString().substring(0,12), "tenDotGiamGia", "Team campaign sync", "phanTramGiamDot", 15,
            "ngayBatDau", "2098-01-01T08:30:00", "ngayKetThuc", "2098-01-31T23:59:00", "kichHoat", true,
            "chiTiet", List.of(Map.of("idSanPhamChiTiet", variant, "phanTramGiamBienThe",20))));
    }
    @Test void latestTeamContractCreatesSearchesUpdatesAndDisablesWithoutChangingProduct() throws Exception {
        MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).build();
        var request=request();
        String json=mvc.perform(post("/api/dot-giam-gia").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.chiTiet[0].phanTramGiamBienThe").value(20)).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(json).get("data").get("id").asLong();
        em.flush();em.clear();
        mvc.perform(get("/api/dot-giam-gia/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.maDotGiamGia").value(request.get("maDotGiamGia")));
        mvc.perform(get("/api/dot-giam-gia/search").param("ma",request.get("maDotGiamGia").toString())).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        request.put("tenDotGiamGia","Updated team campaign");
        mvc.perform(put("/api/dot-giam-gia/"+id).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request))).andExpect(status().isOk());
        em.flush();em.clear();
        mvc.perform(get("/api/dot-giam-gia/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.tenDotGiamGia").value("Updated team campaign")).andExpect(jsonPath("$.data.chiTiet.length()").value(1));
        mvc.perform(delete("/api/dot-giam-gia/"+id)).andExpect(status().isOk());
        em.flush();em.clear();
        mvc.perform(get("/api/dot-giam-gia/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.kichHoat").value(false)).andExpect(jsonPath("$.data.chiTiet.length()").value(0));
    }
    @Test void invalidPercentAndDuplicateVariantsReturn400() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).build();var req=request();req.put("phanTramGiamDot",101);
        mvc.perform(post("/api/dot-giam-gia").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req))).andExpect(status().isBadRequest());
        req.put("phanTramGiamDot",15);var detail=((List<?>)req.get("chiTiet")).get(0);req.put("chiTiet",List.of(detail,detail));
        mvc.perform(post("/api/dot-giam-gia").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req))).andExpect(status().isBadRequest());
    }
}
