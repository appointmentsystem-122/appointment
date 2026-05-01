package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RatingDialogTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
        RatingDialog.resetRatingDialogBlockingShowForTests();
    }

    @Test
    void resultForDialogButton_okUsesStarsOrDefault_andCancelNull() {
        assertThat(RatingDialog.resultForDialogButton(ButtonType.OK, 4, " hi "))
                .isNotNull()
                .extracting(RatingDialog.RatingResult::getStars, RatingDialog.RatingResult::getComment)
                .containsExactly(4, "hi");
        assertThat(RatingDialog.resultForDialogButton(ButtonType.OK, 0, "x").getStars()).isEqualTo(1);
        assertThat(RatingDialog.resultForDialogButton(ButtonType.CANCEL, 3, "c")).isNull();
    }

    @Test
    void show_nullAppointment_returnsEmpty() {
        assertThat(RatingDialog.show(null, null)).isEmpty();
    }

    @Test
    void show_autoDialogs_returnsFiveStarResult() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-r", "N", "e@e.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 7, 1, 10, 0);
        InPersonAppointment a = new InPersonAppointment("aid", u, new TimeSlot(s, s.plusHours(1)), "Loc");
        AtomicReference<Optional<RatingDialog.RatingResult>> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(RatingDialog.show(null, a));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(ref.get()).isPresent();
        assertThat(ref.get().get().getStars()).isEqualTo(5);
        assertThat(ref.get().get().getComment()).isEqualTo("Auto feedback");
    }

    @Test
    void show_autoDialogs_coversAppointmentTypeLabels() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-r2", "N2", "e2@e.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 7, 2, 11, 0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                assertThat(RatingDialog.show(null, new AssessmentAppointment("as", u, slot))).isPresent();
                assertThat(RatingDialog.show(null, new FollowUpAppointment("fu", u, slot, "prior"))).isPresent();
            } catch (Exception e) {
                err.set(e);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).isNull();
    }

    @Test
    void ratingResult_clampsStarsAndTrimsComment() {
        assertThat(new RatingDialog.RatingResult(0, "  c  ").getStars()).isEqualTo(1);
        assertThat(new RatingDialog.RatingResult(9, null).getStars()).isEqualTo(5);
        assertThat(new RatingDialog.RatingResult(3, "  x  ").getComment()).isEqualTo("x");
    }

    @Test
    void show_autoDialogs_virtualAppointment_coversDefaultTypeLabel() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-v", "V", "v@v.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 8, 1, 9, 0);
        VirtualAppointment a = new VirtualAppointment("vid", u, new TimeSlot(s, s.plusHours(1)), "https://meet");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Optional<RatingDialog.RatingResult>> ref = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                ref.set(RatingDialog.show(null, a));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(ref.get()).isPresent();
    }

    @Test
    void show_autoDialogs_nullTimeSlot_emptyDateInSubtitle() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-ns", "N", "ns@ns.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 8, 2, 10, 0);
        InPersonAppointment a = new InPersonAppointment("ns", u, new TimeSlot(s, s.plusHours(1)), "L");
        Field timeSlotF = Appointment.class.getDeclaredField("timeSlot");
        timeSlotF.setAccessible(true);
        timeSlotF.set(a, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Optional<RatingDialog.RatingResult>> ref = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                ref.set(RatingDialog.show(null, a));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(ref.get()).isPresent();
    }

    @Test
    void show_autoDialogs_withOwner_initsOwner() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-o", "O", "o@o.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 8, 3, 11, 0);
        InPersonAppointment a = new InPersonAppointment("oid", u, new TimeSlot(s, s.plusHours(1)), "L");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Optional<RatingDialog.RatingResult>> ref = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Stage owner = new Stage();
                owner.setScene(new Scene(new StackPane(), 1, 1));
                ref.set(RatingDialog.show(owner, a));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(ref.get()).isPresent();
    }

    @Test
    void show_autoDialogs_moreTypes_coversDefaultAppointmentTypeLabel() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-mix", "Mix", "mix@m.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 9, 10, 10, 0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        RecurrencePattern rp = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s.minusWeeks(1), s.plusMonths(3), 1);
        List<Appointment> variety = List.of(
                new GroupAppointment("g1", u, slot, 8),
                new IndividualAppointment("ind1", u, slot),
                new UrgentAppointment("urg1", u, slot),
                new RecurringAppointment("rec1", u, slot, "sid", rp, "oid"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                for (Appointment a : variety) {
                    assertThat(RatingDialog.show(null, a)).as(a.getClass().getSimpleName()).isPresent();
                }
            } catch (Exception e) {
                err.set(e);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).isNull();
    }

    @Test
    void ratingAppointmentTypeLabel_coversAllBranches() {
        User u = new User("u-lbl", "L", "l@l.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 10, 1, 9, 0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        assertThat(RatingDialog.ratingAppointmentTypeLabel(null)).isEqualTo("");
        assertThat(RatingDialog.ratingAppointmentTypeLabel(new AssessmentAppointment("a", u, slot))).isEqualTo("First session");
        assertThat(RatingDialog.ratingAppointmentTypeLabel(new FollowUpAppointment("f", u, slot, "p"))).isEqualTo("Return visit");
        assertThat(RatingDialog.ratingAppointmentTypeLabel(new VirtualAppointment("v", u, slot, "https://x"))).isEqualTo("Virtual");
        assertThat(RatingDialog.ratingAppointmentTypeLabel(new GroupAppointment("g", u, slot, 5))).isEqualTo("Group");
    }

    @Test
    void ratingSubtitleDatePart_formatsWhenSlotPresent_andEmptyWhenMissing() {
        User u = new User("u-sub", "S", "s@s.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 5, 20, 14, 30);
        InPersonAppointment withSlot = new InPersonAppointment("ws", u, new TimeSlot(s, s.plusHours(1)), "L");
        assertThat(RatingDialog.ratingSubtitleDatePart(withSlot)).contains("2026").contains("14:30");

        InPersonAppointment noSlot = new InPersonAppointment("ns", u, new TimeSlot(s, s.plusHours(1)), "L");
        try {
            Field timeSlotF = Appointment.class.getDeclaredField("timeSlot");
            timeSlotF.setAccessible(true);
            timeSlotF.set(noSlot, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        assertThat(RatingDialog.ratingSubtitleDatePart(noSlot)).isEmpty();
    }

    @Test
    void applyRatingOkButtonLabel_setsTextOrNoOpWhenNull() {
        RatingDialog.applyRatingOkButtonLabel(null);
        Button ok = new Button();
        RatingDialog.applyRatingOkButtonLabel(ok);
        assertThat(ok.getText()).contains("Submit");

        javafx.scene.control.DialogPane errPane = new javafx.scene.control.DialogPane();
        assertThatCode(() -> {
            RatingDialog.addRatingDialogStylesheet(errPane, () -> {
                throw new RuntimeException("stylesheet resolution failed");
            });
        }).doesNotThrowAnyException();

        javafx.scene.control.DialogPane nullPane = new javafx.scene.control.DialogPane();
        RatingDialog.addRatingDialogStylesheet(nullPane, () -> null);
        assertThat(nullPane.getStylesheets()).isEmpty();
    }

    @Test
    void starMouseEnteredExitedClicked_branches_onFxThread() throws Exception {
        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            stars[i] = new Label();
            stars[i].getStyleClass().add("rating-star");
            final int idx = i + 1;
            stars[i].setOnMouseEntered(e -> {
                RatingDialog.ratingUpdateStarDisplay(stars, 0, idx);
            });
            stars[i].setOnMouseExited(e -> {
                RatingDialog.ratingUpdateStarDisplay(stars, 0, -1);
            });
            stars[i].setOnMouseClicked(e -> {
                RatingDialog.ratingUpdateStarDisplay(stars, idx, -1);
            });
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                stars[2].fireEvent(mouse(MouseEvent.MOUSE_ENTERED));
                stars[2].fireEvent(mouse(MouseEvent.MOUSE_EXITED));
                stars[4].fireEvent(mouse(MouseEvent.MOUSE_ENTERED));
                stars[4].fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(stars[4].getText()).isEqualTo("\u2605");
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type) {
        return new MouseEvent(
                type,
                0, 0, 0, 0,
                MouseButton.PRIMARY,
                1,
                false, false, false, false,
                false, false, false,
                false, false, false,
                null);
    }

    @Test
    void ratingUpdateStarDisplay_hoverWinsOverSelected() {
        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            stars[i] = new Label();
            stars[i].getStyleClass().add("rating-star");
        }
        RatingDialog.ratingUpdateStarDisplay(stars, 1, 4);
        assertThat(stars[3].getText()).isEqualTo("\u2605");
        assertThat(stars[4].getText()).isEqualTo("\u2606");
        RatingDialog.ratingUpdateStarDisplay(stars, 3, -1);
        assertThat(stars[2].getText()).isEqualTo("\u2605");
        assertThat(stars[3].getText()).isEqualTo("\u2606");
        RatingDialog.ratingUpdateStarDisplay(stars, 0, -1);
        assertThat(stars[0].getText()).isEqualTo("\u2606");
        assertThat(stars[4].getText()).isEqualTo("\u2606");
    }

    @Test
    void show_autoDialogs_emptyTypeLabel_coversSubtitleConcatBranch() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User u = new User("u-empty-type", "E", "e@e.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 12, 1, 9, 0);
        var appt = new com.appointmentscheduler.presentation.fixtures.Appointment("eid", u, new TimeSlot(s, s.plusHours(1)));
        assertThat(RatingDialog.ratingAppointmentTypeLabel(appt)).isEmpty();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                assertThat(RatingDialog.show(null, appt)).isPresent();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).isNull();
    }

    @Test
    void show_nonAuto_starHandlersAndConverter_submit() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        RatingDialog.ratingDialogBlockingShow = d -> {
            javafx.scene.control.DialogPane pane = d.getDialogPane();
            Node content = pane.getContent();
            if (content instanceof VBox v) {
                HBox stars = findRatingStarsBox(v);
                if (stars != null && stars.getChildren().size() >= 5) {
                    Label s3 = (Label) stars.getChildren().get(2);
                    Label s5 = (Label) stars.getChildren().get(4);
                    s3.fireEvent(mouse(MouseEvent.MOUSE_ENTERED));
                    s3.fireEvent(mouse(MouseEvent.MOUSE_EXITED));
                    s5.fireEvent(mouse(MouseEvent.MOUSE_ENTERED));
                    s5.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));
                    s5.fireEvent(mouse(MouseEvent.MOUSE_EXITED));
                    TextArea ta = findCommentArea(v);
                    if (ta != null) {
                        ta.setText("  typed  ");
                    }
                }
            }
            return Optional.ofNullable(d.getResultConverter().call(ButtonType.OK));
        };

        User u = new User("u-nd", "D", "d@d.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 11, 2, 15, 0);
        InPersonAppointment a = new InPersonAppointment("nid", u, new TimeSlot(s, s.plusHours(1)), "Loc");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        AtomicReference<Optional<RatingDialog.RatingResult>> res = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                res.set(RatingDialog.show(null, a));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(25, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).isNull();
        assertThat(res.get()).isPresent();
        assertThat(res.get().get().getStars()).isEqualTo(5);
        assertThat(res.get().get().getComment()).isEqualTo("typed");
    }

    @Test
    void show_nonAuto_emptyAppointmentType_subtitleBranch_andOk() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        RatingDialog.ratingDialogBlockingShow = d ->
                Optional.ofNullable(d.getResultConverter().call(ButtonType.OK));

        User u = new User("u-net", "E", "net@e.com", "h");
        LocalDateTime s = LocalDateTime.of(2026, 12, 5, 10, 0);
        var appt = new com.appointmentscheduler.presentation.fixtures.Appointment("e2", u, new TimeSlot(s, s.plusHours(1)));
        assertThat(RatingDialog.ratingAppointmentTypeLabel(appt)).isEmpty();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        AtomicReference<Optional<RatingDialog.RatingResult>> res = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                res.set(RatingDialog.show(null, appt));
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(25, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).isNull();
        assertThat(res.get()).isPresent();
        assertThat(res.get().get().getStars()).isEqualTo(1);
    }

    @Test
    void show_nonAuto_converter_cancelEmpty_then_okDefaultsToOneStar() throws Exception {
        System.clearProperty("app.test.autoDialogs");

        RatingDialog.ratingDialogBlockingShow = d ->
                Optional.ofNullable(d.getResultConverter().call(ButtonType.CANCEL));
        User uCancel = new User("u-nc", "C", "c@c.com", "h");
        LocalDateTime sCancel = LocalDateTime.of(2026, 11, 3, 11, 30);
        InPersonAppointment apptCancel = new InPersonAppointment("cid", uCancel, new TimeSlot(sCancel, sCancel.plusHours(1)), "L");
        CountDownLatch latchCancel = new CountDownLatch(1);
        AtomicReference<Throwable> errCancel = new AtomicReference<>();
        AtomicReference<Optional<RatingDialog.RatingResult>> resCancel = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                resCancel.set(RatingDialog.show(null, apptCancel));
            } catch (Throwable t) {
                errCancel.set(t);
            } finally {
                latchCancel.countDown();
            }
        });
        assertThat(latchCancel.await(25, TimeUnit.SECONDS)).isTrue();
        assertThat(errCancel.get()).isNull();
        assertThat(resCancel.get()).isEmpty();

        RatingDialog.ratingDialogBlockingShow = d ->
                Optional.ofNullable(d.getResultConverter().call(ButtonType.OK));
        User uOk = new User("u-n1", "One", "1@1.com", "h");
        LocalDateTime sOk = LocalDateTime.of(2026, 11, 5, 9, 15);
        InPersonAppointment apptOk = new InPersonAppointment("nid1", uOk, new TimeSlot(sOk, sOk.plusHours(1)), "Loc");
        CountDownLatch latchOk = new CountDownLatch(1);
        AtomicReference<Throwable> errOk = new AtomicReference<>();
        AtomicReference<Optional<RatingDialog.RatingResult>> resOk = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                resOk.set(RatingDialog.show(null, apptOk));
            } catch (Throwable t) {
                errOk.set(t);
            } finally {
                latchOk.countDown();
            }
        });
        assertThat(latchOk.await(25, TimeUnit.SECONDS)).isTrue();
        assertThat(errOk.get()).isNull();
        assertThat(resOk.get()).isPresent();
        assertThat(resOk.get().get().getStars()).isEqualTo(1);
        assertThat(resOk.get().get().getComment()).isEmpty();
    }

    private static HBox findRatingStarsBox(VBox root) {
        for (Node ch : root.getChildren()) {
            if (ch instanceof HBox h && h.getStyleClass().contains("rating-stars-box")) {
                return h;
            }
        }
        return null;
    }

    private static TextArea findCommentArea(VBox root) {
        for (Node ch : root.getChildren()) {
            if (ch instanceof VBox vb) {
                for (Node inner : vb.getChildren()) {
                    if (inner instanceof TextArea ta) {
                        return ta;
                    }
                }
            }
        }
        return null;
    }
}
