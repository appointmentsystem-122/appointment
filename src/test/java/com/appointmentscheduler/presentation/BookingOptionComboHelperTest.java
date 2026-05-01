package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppointmentTypeConfig;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BookingOptionComboHelperTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void configureNullNoOp() {
        assertThatCode(() -> BookingOptionComboHelper.configure(null)).doesNotThrowAnyException();
    }

    @Test
    void listCellTextForBookingOption_coversBranches() {
        BookingOption opt = BookingOption.of(new AppointmentTypeConfig.Type("T", 30, 1), true);
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(null, true)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(null, false)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(opt, true)).isNull();
        assertThat(BookingOptionComboHelper.listCellTextForBookingOption(opt, false)).isEqualTo(opt.getDisplayLabel());
    }

    @Test
    void configure_setsDropdownAndButtonCellFactories() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ComboBox<BookingOption> combo = new ComboBox<>();
                BookingOptionComboHelper.configure(combo);
                assertThat(combo.getCellFactory()).isNotNull();
                ListView<BookingOption> lv = new ListView<>();
                ListCell<BookingOption> dropCell = combo.getCellFactory().call(lv);
                assertThat(dropCell).isNotNull();
                assertThat(combo.getButtonCell()).isNotNull();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
