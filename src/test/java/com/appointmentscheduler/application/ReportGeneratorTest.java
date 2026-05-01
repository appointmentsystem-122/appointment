package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportGenerator")
class ReportGeneratorTest {

    @Test
    @DisplayName("Utility class can be instantiated (default ctor)")
    void constructor_isCovered() {
        assertThat(new ReportGenerator()).isNotNull();
    }

    @Test
    @DisplayName("Writes header and rows; succeeds on temp file path")
    void exportCsv_writesFile() throws Exception {
        User p = new User("u", "NameX", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L");
        Path f = Files.createTempFile("reptest", ".csv");
        try {
            assertThat(ReportGenerator.exportAppointmentsToCSV(Collections.singletonList(a), f.toString())).isTrue();
            String content = Files.readString(f);
            assertThat(content).contains("NameX").contains("Appointment ID").contains("InPersonAppointment");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    @DisplayName("Empty list still writes header and returns true")
    void emptyList_writesHeaderOnly() throws Exception {
        Path f = Files.createTempFile("repempty", ".csv");
        try {
            assertThat(ReportGenerator.exportAppointmentsToCSV(List.of(), f.toString())).isTrue();
            assertThat(Files.readString(f)).contains("Appointment ID").doesNotContain("InPersonAppointment");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    @DisplayName("Commas in patient name are replaced to keep CSV columns stable")
    void commaInNameSanitized() throws Exception {
        User p = new User("u", "Last, First", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L");
        Path f = Files.createTempFile("repcomma", ".csv");
        try {
            assertThat(ReportGenerator.exportAppointmentsToCSV(List.of(a), f.toString())).isTrue();
            assertThat(Files.readString(f)).doesNotContain("Last, First").contains("Last  First");
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    @DisplayName("Invalid file path returns false")
    void invalidPath_returnsFalse() {
        User p = new User("u", "NameX", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L");
        boolean ok = ReportGenerator.exportAppointmentsToCSV(List.of(a), "?:\\invalid\\report.csv");
        assertThat(ok).isFalse();
    }
}
