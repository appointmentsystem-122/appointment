package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDialogAutoTest {

    @BeforeAll
    static void setup() {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void show_autoMode_returnsSubmittedRatingWithoutBlocking() throws Exception {
        User patient = new User("u-1", "Test Patient", "p@example.com", "pw");
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        TimeSlot slot = new TimeSlot(start, start.plusMinutes(30));
        AssessmentAppointment appt = new AssessmentAppointment(patient, slot);

        Optional<RatingDialog.RatingResult> res = runOnFx(() -> RatingDialog.show((Window) null, appt));

        assertThat(res).isPresent();
        assertThat(res.get().getStars()).isEqualTo(5);
        assertThat(res.get().getComment()).isEqualTo("Auto feedback");
    }

    private static <T> T runOnFx(Callable<T> task) throws Exception {
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
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX task timed out");
        }
        if (err.get() != null) throw new RuntimeException(err.get());
        return ref.get();
    }
}

