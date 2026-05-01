package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppointmentTypeConfig;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One batch of branch-coverage boosts for presentation classes that JaCoCo reports under ~80%
 * ({@link BookingOptionComboHelper}, {@link RatingDialog} helpers, etc.).
 */
@ResourceLock("ApplicationContextServices")
@ResourceLock("AppConfigProps")
class PresentationBranchBelow80UnifiedBoostTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void bookingOptionComboHelper_cellFactory_and_buttonCell_coverAllUpdateItemBranches() {
        BookingOption opt = BookingOption.of(new AppointmentTypeConfig.Type("Svc", 30, 2), true);
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(null, true)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(null, false)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(opt, true)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(opt, false)).isEqualTo(opt.getDisplayLabel());

        ComboBox<BookingOption> combo = new ComboBox<>();
        BookingOptionComboHelper.configure(combo);
        assertThat(combo.getCellFactory()).isNotNull();
        assertThat(combo.getButtonCell()).isNotNull();
    }

    @Test
    void ratingDialog_ratingSubtitleDatePart_nullSlot_returnsEmpty() throws Exception {
        User u = new User("u-rsub", "R", "rsub@e.com", "pw");
        LocalDateTime s = LocalDateTime.of(2030, 3, 1, 10, 0);
        InPersonAppointment appt = new InPersonAppointment("a-rsub", u, new TimeSlot(s, s.plusHours(1)), "L");
        Field f = com.appointmentscheduler.domain.Appointment.class.getDeclaredField("timeSlot");
        f.setAccessible(true);
        f.set(appt, null);
        assertThat(RatingDialog.ratingSubtitleDatePart(appt)).isEmpty();
    }

    @Test
    void ratingDialog_ratingSubtitleDatePart_withSlot_includesFormattedDate() {
        User u = new User("u-rsub2", "R", "rsub2@e.com", "pw");
        LocalDateTime s = LocalDateTime.of(2031, 7, 15, 14, 30);
        InPersonAppointment appt = new InPersonAppointment("a-r2", u, new TimeSlot(s, s.plusHours(1)), "L");
        assertThat(RatingDialog.ratingSubtitleDatePart(appt)).contains("2031").contains("14:30");
    }

    @Test
    void ratingDialog_resultForDialogButton_coversOkDefaultStars_andCancel() {
        RatingDialog.RatingResult okZero = RatingDialog.resultForDialogButton(ButtonType.OK, 0, "  x  ");
        assertThat(okZero).isNotNull();
        assertThat(okZero.getStars()).isEqualTo(1);
        assertThat(okZero.getComment()).isEqualTo("x");

        RatingDialog.RatingResult okFour = RatingDialog.resultForDialogButton(ButtonType.OK, 4, null);
        assertThat(okFour.getStars()).isEqualTo(4);
        assertThat(okFour.getComment()).isEmpty();

        assertThat(RatingDialog.resultForDialogButton(ButtonType.CANCEL, 3, "c")).isNull();
    }
}
