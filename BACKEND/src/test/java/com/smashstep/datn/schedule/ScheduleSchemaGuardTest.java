package com.smashstep.datn.schedule;

import com.smashstep.datn.common.config.DatabaseCapabilities;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.service.ScheduleAvailability;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleSchemaGuardTest {

    final DatabaseCapabilities database = mock(DatabaseCapabilities.class);
    final ScheduleAvailability availability = new ScheduleAvailability(database);

    @Test
    void absentTablesAreDetectedWithoutAnySchemaCreation() {
        // No columns reported → feature unavailable
        assertFalse(availability.available());
        assertThrows(AppException.class, availability::requireAvailable);
    }

    @Test
    void completeExistingColumnsEnableTheTeamFeature() {
        // All columns present → feature available
        when(database.hasColumn(anyString(), anyString())).thenReturn(true);
        assertTrue(availability.available());
    }

    @Test
    void partialColumnsKeepFeatureUnavailable() {
        // Only ca_lam_viec columns present, lich_lam_viec missing → still unavailable
        when(database.hasColumn(eq("ca_lam_viec"), anyString())).thenReturn(true);
        when(database.hasColumn(eq("lich_lam_viec"), anyString())).thenReturn(false);
        assertFalse(availability.available());
    }

    @Test
    void requireAvailableDoesNotThrowWhenAllColumnsPresent() {
        when(database.hasColumn(anyString(), anyString())).thenReturn(true);
        assertDoesNotThrow(availability::requireAvailable);
    }
}
