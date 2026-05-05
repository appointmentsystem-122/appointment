package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.print.PrinterJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintHelperCoverageTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void printReceipt_nullAppointment_returnsImmediately() {
        assertThatCode(() -> PrintHelper.printAppointmentReceipt(null, null)).doesNotThrowAnyException();
    }

    @Test
    void printReceipt_autoDialogs_shortCircuitPath() {
        System.setProperty("app.test.autoDialogs", "true");
        IndividualAppointment appt = sampleAppointment();
        assertThatCode(() -> JavaFxTestSupport.runOnFxThread(
                () -> PrintHelper.printAppointmentReceipt(appt, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void printReceipt_printerUnavailable_showsError() {
        IndividualAppointment appt = sampleAppointment();
        JavaFxTestSupport.runOnFxThread(() -> {
            try (MockedStatic<PrinterJob> pj = mockStatic(PrinterJob.class);
                 MockedStatic<DialogHelper> dh = mockStatic(DialogHelper.class)) {
                pj.when(PrinterJob::createPrinterJob).thenReturn(null);
                dh.when(() -> DialogHelper.showError(anyString(), anyString())).thenAnswer(i -> null);
                PrintHelper.printAppointmentReceipt(appt, null);
                dh.verify(() -> DialogHelper.showError(anyString(), anyString()));
            }
        });
    }

    @Test
    void printReceipt_nonAuto_userCancelsDialog_doesNotPrint() {
        System.clearProperty("app.test.autoDialogs");
        IndividualAppointment appt = sampleAppointment();
        PrinterJob job = mock(PrinterJob.class);
        when(job.showPrintDialog(null)).thenReturn(false);
        JavaFxTestSupport.runOnFxThread(() -> {
            try (MockedStatic<PrinterJob> pj = mockStatic(PrinterJob.class)) {
                pj.when(PrinterJob::createPrinterJob).thenReturn(job);
                PrintHelper.printAppointmentReceipt(appt, null);
                verify(job, never()).printPage(any());
                verify(job, never()).endJob();
            }
        });
    }

    @Test
    void printReceipt_nonAuto_printedFalse_showsFailurePath() {
        System.clearProperty("app.test.autoDialogs");
        IndividualAppointment appt = sampleAppointment();
        PrinterJob job = mock(PrinterJob.class);
        when(job.showPrintDialog(null)).thenReturn(true);
        when(job.printPage(any())).thenReturn(false);
        JavaFxTestSupport.runOnFxThread(() -> {
            try (MockedStatic<PrinterJob> pj = mockStatic(PrinterJob.class)) {
                pj.when(PrinterJob::createPrinterJob).thenReturn(job);
                PrintHelper.printAppointmentReceipt(appt, null);
                verify(job).printPage(any());
                verify(job, never()).endJob();
            }
        });
    }

    @Test
    void printReceipt_nonAuto_printedTrue_endsJob() {
        System.clearProperty("app.test.autoDialogs");
        IndividualAppointment appt = sampleAppointment();
        PrinterJob job = mock(PrinterJob.class);
        when(job.showPrintDialog(null)).thenReturn(true);
        when(job.printPage(any())).thenReturn(true);
        JavaFxTestSupport.runOnFxThread(() -> {
            try (MockedStatic<PrinterJob> pj = mockStatic(PrinterJob.class)) {
                pj.when(PrinterJob::createPrinterJob).thenReturn(job);
                PrintHelper.printAppointmentReceipt(appt, null);
                verify(job).printPage(any());
                verify(job).endJob();
            }
        });
    }

    private static IndividualAppointment sampleAppointment() {
        User u = new User("u-print", "Print User", "print@example.com", "pw");
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        IndividualAppointment appt = new IndividualAppointment(u, slot);
        appt.setStatus("CONFIRMED");
        return appt;
    }
}
