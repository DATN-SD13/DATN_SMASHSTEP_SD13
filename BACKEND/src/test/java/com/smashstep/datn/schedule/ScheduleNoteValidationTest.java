package com.smashstep.datn.schedule;

import com.smashstep.datn.schedule.dto.CapNhatTrangThaiRequest;
import com.smashstep.datn.schedule.dto.LichLamViecRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleNoteValidationTest {
    @Test void createNoteMatchesHungNvarchar500Column(){
        try(var factory=Validation.buildDefaultValidatorFactory()){
            var validator=factory.getValidator();var request=new LichLamViecRequest();
            request.setMaNhanViens(List.of("NV0001"));request.setCaLamViecId(1L);request.setNgayLam(LocalDate.now());
            request.setGhiChu("x".repeat(500));assertTrue(validator.validate(request).isEmpty());
            request.setGhiChu("x".repeat(501));
            assertTrue(validator.validate(request).stream().anyMatch(v->v.getPropertyPath().toString().equals("ghiChu")));
        }
    }
    @Test void statusNoteAlsoMatchesHungNvarchar500Column(){
        try(var factory=Validation.buildDefaultValidatorFactory()){
            var validator=factory.getValidator();var request=new CapNhatTrangThaiRequest();request.setTrangThai(1);
            request.setGhiChu("x".repeat(500));assertTrue(validator.validate(request).isEmpty());
            request.setGhiChu("x".repeat(501));
            assertTrue(validator.validate(request).stream().anyMatch(v->v.getPropertyPath().toString().equals("ghiChu")));
        }
    }
}
