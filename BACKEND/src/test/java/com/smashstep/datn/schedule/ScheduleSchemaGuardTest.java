package com.smashstep.datn.schedule;

import com.smashstep.datn.common.config.DatabaseCapabilities;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.repository.*;
import com.smashstep.datn.schedule.service.ScheduleAvailability;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleSchemaGuardTest {
    final DatabaseCapabilities database=mock(DatabaseCapabilities.class);
    final ScheduleAvailability availability=new ScheduleAvailability(database);
    @Test void absentTablesAreDetectedWithoutAnySchemaCreation(){assertFalse(availability.available());assertThrows(AppException.class,availability::requireAvailable);}
    @Test void completeExistingColumnsEnableTheTeamFeature(){when(database.hasColumn(anyString(),anyString())).thenReturn(true);assertTrue(availability.available());}
    @Test void shiftReadsFailBeforeQueryingMissingTables(){var jdbc=mock(JdbcTemplate.class);var repo=new CaLamViecRepository(jdbc,availability);assertThrows(AppException.class,repo::findAll);assertThrows(AppException.class,()->repo.findById(1L));verifyNoInteractions(jdbc);}
    @Test void scheduleReadsFailBeforeQueryingMissingTables(){var jdbc=mock(NamedParameterJdbcTemplate.class);var repo=new LichLamViecRepository(jdbc,availability);assertThrows(AppException.class,()->repo.search(null,null,null,null,PageRequest.of(0,10)));assertThrows(AppException.class,()->repo.findById(1L));verifyNoInteractions(jdbc);}
}
