package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AppointmentTypeConfig;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.ClosedDayService;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class PresentationUtilityCoverageBoost2Test {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void csvUtils_escape_handlesNullQuotesAndPlainText() {
        assertThat(CsvUtils.escape(null)).isEqualTo("\"\"");
        assertThat(CsvUtils.escape("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(CsvUtils.escape(123)).isEqualTo("\"123\"");
    }

    @Test
    void bookingDateMessages_unavailable_handlesClosedAndDefaultCases() {
        ClosedDayService cds = new ClosedDayService();
        LocalDate d = LocalDate.of(2026, 4, 12);
        cds.addClosedDay(d);
        ApplicationContext.setClosedDayService(cds);
        assertThat(BookingDateMessages.unavailable(d)).isNotBlank();
        assertThat(BookingDateMessages.unavailable(d.plusDays(1))).isNotBlank();
        ApplicationContext.setClosedDayService(null);
        assertThat(BookingDateMessages.unavailable(d)).isNotBlank();
    }

    @Test
    void appNotificationStore_branches_forRecentUnreadAndEntryDefaults() {
        AppNotificationStore s = new AppNotificationStore();
        s.clear();
        assertThat(s.getUnreadCount()).isZero();

        AppNotificationStore.Entry e = new AppNotificationStore.Entry(null, null, null, true);
        assertThat(e.getTitle()).isEmpty();
        assertThat(e.getMessage()).isEmpty();
        assertThat(e.getTimeFormatted()).isNotBlank();
        assertThat(e.isError()).isTrue();

        for (int i = 0; i < 120; i++) {
            s.add("t" + i, "m" + i, i % 2 == 0);
        }
        assertThat(s.getUnreadCount()).isEqualTo(50);

        List<AppNotificationStore.Entry> full = s.getRecent(100);
        assertThat(full).hasSize(50);
        List<AppNotificationStore.Entry> tail = s.getRecent(5);
        assertThat(tail).hasSize(5);
        ObservableList<AppNotificationStore.Entry> obs = s.getObservableRecent(3);
        assertThat(obs).hasSize(3);
    }

    @Test
    void bookingExtrasUi_labels_coverSwitchAndNullBranches() {
        assertThat(BookingExtrasUi.reminderChannelLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_EMAIL)).contains("Email");
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_SMS)).contains("SMS");
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_NONE)).contains("None");
        assertThat(BookingExtrasUi.reminderChannelLabel("UNKNOWN")).contains("App");

        assertThat(BookingExtrasUi.preferredLanguageLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.preferredLanguageLabel(BookingRequestFields.LANG_AR)).contains("Arabic");
        assertThat(BookingExtrasUi.preferredLanguageLabel(BookingRequestFields.LANG_EN)).contains("English");
        assertThat(BookingExtrasUi.preferredLanguageLabel("ANY")).contains("No preference");
    }

    @Test
    void bookingExtrasUi_configureCombos_and_updateSpinner_branches() {
        runOnFxVoid(() -> {
            ComboBox<String> reminder = new ComboBox<>();
            BookingExtrasUi.configureReminderCombo(reminder);
            assertThat(reminder.getItems()).hasSize(3);
            assertThat(reminder.getValue()).isEqualTo(BookingRequestFields.REMINDER_APP);
            // second call early-return branch
            BookingExtrasUi.configureReminderCombo(reminder);
            assertThat(reminder.getItems()).hasSize(3);
            assertThat(reminder.getConverter().toString(BookingRequestFields.REMINDER_SMS)).contains("SMS");
            assertThat(reminder.getConverter().fromString("x")).isEqualTo(BookingRequestFields.REMINDER_APP);
            BookingExtrasUi.configureReminderCombo(null);

            ComboBox<String> lang = new ComboBox<>();
            BookingExtrasUi.configureLanguageCombo(lang);
            assertThat(lang.getItems()).hasSize(3);
            assertThat(lang.getValue()).isEqualTo(BookingRequestFields.LANG_ANY);
            BookingExtrasUi.configureLanguageCombo(lang);
            assertThat(lang.getConverter().toString(BookingRequestFields.LANG_AR)).contains("Arabic");
            assertThat(lang.getConverter().fromString("x")).isEqualTo(BookingRequestFields.LANG_ANY);
            BookingExtrasUi.configureLanguageCombo(null);

            Spinner<Integer> sp = new Spinner<>();
            // null value branch
            BookingExtrasUi.updatePartySpinner(sp, 1);
            assertThat(sp.getValue()).isEqualTo(1);
            assertThat(sp.isEditable()).isFalse();
            assertThat(sp.getTooltip()).isNotNull();

            sp.getValueFactory().setValue(5);
            BookingExtrasUi.updatePartySpinner(sp, 3);
            assertThat(sp.getValue()).isEqualTo(1);
            assertThat(sp.isEditable()).isTrue();
            assertThat(sp.getTooltip()).isNull();

            Spinner<Integer> broken = new Spinner<>();
            // exception path in getValue (no value factory set)
            BookingExtrasUi.updatePartySpinner(broken, 2);
            assertThat(broken.getValue()).isEqualTo(1);

            BookingExtrasUi.updatePartySpinner(null, 2);
        });
    }

    @Test
    void loadingSpinnerOverlay_attach_show_hide_branches() {
        runOnFxVoid(() -> {
            LoadingSpinnerOverlay overlay = new LoadingSpinnerOverlay();
            StackPane pane = new StackPane();
            overlay.attachTo(pane);
            assertThat(pane.getChildren()).hasSize(1);
            overlay.attachTo(pane); // already attached branch
            assertThat(pane.getChildren()).hasSize(1);
            overlay.show();
            overlay.hide();
        });
    }

    @Test
    void bookingOptionComboHelper_configure_nullAndUpdateCellPaths() {
        runOnFxVoid(() -> {
            BookingOptionComboHelper.configure(null);

            ComboBox<BookingOption> combo = new ComboBox<>();
            BookingOptionComboHelper.configure(combo);
            assertThat(combo.getCellFactory()).isNotNull();
            assertThat(combo.getButtonCell()).isNotNull();

            BookingOption option = BookingOption.of(new AppointmentTypeConfig.Type("Consult", 45, 2), true);
            combo.getItems().add(option);
            combo.getSelectionModel().select(option);
            combo.layout();
            assertThat(combo.getSelectionModel().getSelectedItem()).isEqualTo(option);
        });
    }

    @Test
    void screenConstants_titles_areCovered() {
        try (var appCfg = mockStatic(AppConfig.class)) {
            appCfg.when(AppConfig::getAppName).thenReturn("AppointmentX");
            assertThat(ScreenConstants.titleLogin()).contains("AppointmentX");
            assertThat(ScreenConstants.titleAdminDashboard()).contains("AppointmentX");
            assertThat(ScreenConstants.titlePatientDashboard()).contains("AppointmentX");
            assertThat(ScreenConstants.titleBookAppointment()).contains("AppointmentX");
            assertThat(ScreenConstants.titleModifyAppointment()).contains("AppointmentX");
            assertThat(ScreenConstants.BASE_PATH).contains("/presentation/");
        }
    }

    private static <T> T runOnFx(Callable<T> task) {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(task.call());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) throw new RuntimeException(err.get());
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }
}

