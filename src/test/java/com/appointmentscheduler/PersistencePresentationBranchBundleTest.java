package com.appointmentscheduler;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ClosedDayService;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryAuditEntryRepository;
import com.appointmentscheduler.persistence.InMemoryClinicRepository;
import com.appointmentscheduler.persistence.InMemoryDoctorRepository;
import com.appointmentscheduler.persistence.InMemoryRoomRepository;
import com.appointmentscheduler.persistence.InMemoryUserRepository;
import com.appointmentscheduler.persistence.database.JdbcPostgresHelper;
import com.appointmentscheduler.presentation.AppNotificationStore;
import com.appointmentscheduler.presentation.BookingDateMessages;
import com.appointmentscheduler.presentation.I18n;
import com.appointmentscheduler.presentation.ScreenConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Single batch targeting branch coverage in {@code com.appointmentscheduler.persistence}
 * and {@code com.appointmentscheduler.presentation} (in-memory filters, notification store caps,
 * booking message guards, screen title wiring, JDBC helper edge).
 */
@ResourceLock("AppConfigProps")
class PersistencePresentationBranchBundleTest {

    @AfterEach
    void resetClosedDayContext() {
        ApplicationContext.setClosedDayService(null);
        Preferences p = Preferences.userNodeForPackage(ClosedDayService.class);
        p.remove("admin.closedDays");
        try {
            p.flush();
        } catch (BackingStoreException ignored) {
        }
    }

    // --- persistence (in-memory filters & optional lookups) ---

    @Test
    void audit_findByUserId_noMatches_returnsEmpty() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(new AuditEntry(LocalDateTime.now(), "u-a", "N", "act", "d"));
        assertThat(repo.findByUserId("other-user")).isEmpty();
    }

    @Test
    void audit_findByEntityType_noMatches_returnsEmpty() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(new AuditEntry(LocalDateTime.now(), "u", "N", "act", "d", "BOOK", "e1", "o", "n"));
        assertThat(repo.findByEntityType("OTHER_TYPE")).isEmpty();
    }

    @Test
    void user_findById_unknown_returnsEmpty() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        repo.save(new User("id-1", "N", "e@x.com", "h"));
        assertThat(repo.findById("missing-id")).isEmpty();
    }

    @Test
    void clinic_findById_unknown_returnsEmpty() {
        InMemoryClinicRepository repo = new InMemoryClinicRepository();
        repo.save(new Clinic("c1", "Main", "addr", "UTC"));
        assertThat(repo.findById("other")).isEmpty();
    }

    @Test
    void doctor_findById_unknown_returnsEmpty() {
        InMemoryDoctorRepository repo = new InMemoryDoctorRepository();
        repo.save(new Doctor("d1", "Dr", "d@x.com", "S", 3));
        assertThat(repo.findById("ghost")).isEmpty();
    }

    @Test
    void room_findById_unknown_returnsEmpty() {
        InMemoryRoomRepository repo = new InMemoryRoomRepository();
        repo.save(new Room("r1", "R1"));
        assertThat(repo.findById("ghost-room")).isEmpty();
    }

    @Test
    void jdbcPostgresHelper_productUppercasePostgresql_detected() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("POSTGRESQL");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isTrue();
        assertThat(JdbcPostgresHelper.table(c, "x")).isEqualTo("appointment.x");
    }

    // --- presentation ---

    @Test
    void appNotificationStore_entry_nullFields_useDefaults() {
        AppNotificationStore.Entry e = new AppNotificationStore.Entry(null, null, null, true);
        assertThat(e.getTitle()).isEmpty();
        assertThat(e.getMessage()).isEmpty();
        assertThat(e.getTimeFormatted()).isNotBlank();
        assertThat(e.isError()).isTrue();
    }

    @Test
    void appNotificationStore_unreadCount_trimsToMaxItems_thenCountsAllRetained() {
        AppNotificationStore store = new AppNotificationStore();
        IntStream.range(0, 30).forEach(i -> store.add("t", "m"));
        assertThat(store.getUnreadCount()).isEqualTo(30);
        // Store caps at MAX_ITEMS (50); getUnreadCount is min(size, 99) so it returns full retained size.
        IntStream.range(0, 40).forEach(i -> store.add("t2", "m"));
        assertThat(store.getUnreadCount()).isEqualTo(50);
    }

    @Test
    void bookingDateMessages_closedDay_matchesClosedKey_openDay_matchesNoSlotsKey() {
        LocalDate open = LocalDate.of(2030, 3, 10);
        ClosedDayService cds = new ClosedDayService();
        ApplicationContext.setClosedDayService(cds);
        assertThat(BookingDateMessages.unavailable(open)).isEqualTo(I18n.get("booking.day_no_slots"));

        LocalDate closed = LocalDate.of(2030, 12, 25);
        cds.addClosedDay(closed);
        assertThat(BookingDateMessages.unavailable(closed)).isEqualTo(I18n.get("booking.day_closed"));
    }

    @Test
    void screenConstants_titlesIncludeAppName() {
        try (MockedStatic<AppConfig> cfg = mockStatic(AppConfig.class)) {
            cfg.when(AppConfig::getAppName).thenReturn("BranchBundleApp");
            assertThat(ScreenConstants.titleLogin()).isEqualTo("Login - BranchBundleApp");
            assertThat(ScreenConstants.titleAdminDashboard()).isEqualTo("Admin - BranchBundleApp");
            assertThat(ScreenConstants.titlePatientDashboard()).isEqualTo("Client - BranchBundleApp");
            assertThat(ScreenConstants.titleBookAppointment()).isEqualTo("Book Appointment - BranchBundleApp");
            assertThat(ScreenConstants.titleModifyAppointment()).isEqualTo("Modify Appointment - BranchBundleApp");
        }
    }
}
