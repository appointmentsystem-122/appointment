package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Focused tests for RatingDialog lines highlighted in the SonarCloud new-code view. */
class RatingDialogScreenshotCoverageBoostTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void resetHook() {
        RatingDialog.resetRatingDialogBlockingShowForTests();
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void nonAutoShow_canDriveStarHandlersCommentAndOkConverter() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        RatingDialog.ratingDialogBlockingShow = dialog -> {
            DialogPane pane = dialog.getDialogPane();
            assertThat(pane.getButtonTypes()).contains(ButtonType.OK, ButtonType.CANCEL);
            Node content = pane.getContent();
            assertThat(content).isInstanceOf(VBox.class);

            VBox root = (VBox) content;
            HBox stars = findStarsBox(root);
            assertThat(stars).isNotNull();
            assertThat(stars.getChildren()).hasSize(5);

            Label fourth = (Label) stars.getChildren().get(3);
            fourth.fireEvent(mouse(MouseEvent.MOUSE_ENTERED));
            fourth.fireEvent(mouse(MouseEvent.MOUSE_EXITED));
            fourth.fireEvent(mouse(MouseEvent.MOUSE_CLICKED));

            TextArea comment = findCommentArea(root);
            assertThat(comment).isNotNull();
            comment.setText("  excellent visit  ");

            return Optional.ofNullable(dialog.getResultConverter().call(ButtonType.OK));
        };

        AtomicReference<Optional<RatingDialog.RatingResult>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(RatingDialog.show(null, sampleAppointment()));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isNull();
        assertThat(result.get()).isPresent();
        assertThat(result.get().get().getStars()).isEqualTo(4);
        assertThat(result.get().get().getComment()).isEqualTo("excellent visit");
    }

    @Test
    void nonAutoShow_cancelAndDefaultStarsBranches_areCovered() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        RatingDialog.ratingDialogBlockingShow = dialog -> Optional.ofNullable(dialog.getResultConverter().call(ButtonType.OK));
        Optional<RatingDialog.RatingResult> defaultResult = showOnFx(sampleAppointment());
        assertThat(defaultResult).isPresent();
        assertThat(defaultResult.get().getStars()).isEqualTo(1);

        RatingDialog.ratingDialogBlockingShow = dialog -> Optional.ofNullable(dialog.getResultConverter().call(ButtonType.CANCEL));
        assertThat(showOnFx(sampleAppointment())).isEmpty();
    }

    @Test
    void stylesheetAndStarDisplayEdgeBranches_areCovered() {
        DialogPane pane = new DialogPane();
        assertThatCode(() -> RatingDialog.addRatingDialogStylesheet(pane, () -> {
            throw new IllegalStateException("css lookup failed");
        })).doesNotThrowAnyException();

        DialogPane paneWithNullUrl = new DialogPane();
        RatingDialog.addRatingDialogStylesheet(paneWithNullUrl, () -> null);
        assertThat(paneWithNullUrl.getStylesheets()).isEmpty();

        Label[] labels = new Label[5];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
            labels[i].getStyleClass().add("rating-star");
        }
        RatingDialog.ratingUpdateStarDisplay(labels, 2, 5);
        assertThat(labels[4].getText()).isEqualTo("★");
        RatingDialog.ratingUpdateStarDisplay(labels, 3, -1);
        assertThat(labels[2].getText()).isEqualTo("★");
        assertThat(labels[3].getText()).isEqualTo("☆");
    }

    private static Optional<RatingDialog.RatingResult> showOnFx(InPersonAppointment appointment) throws Exception {
        AtomicReference<Optional<RatingDialog.RatingResult>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(RatingDialog.show(null, appointment));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
        return result.get();
    }

    private static InPersonAppointment sampleAppointment() {
        User user = new User("u-rating-boost", "Rating User", "rating@example.com", "pw");
        LocalDateTime start = LocalDateTime.of(2026, 5, 7, 13, 30);
        return new InPersonAppointment("rating-boost", user, new TimeSlot(start, start.plusMinutes(45)), "Room A");
    }

    private static HBox findStarsBox(VBox root) {
        for (Node child : root.getChildren()) {
            if (child instanceof HBox box && box.getStyleClass().contains("rating-stars-box")) {
                return box;
            }
        }
        return null;
    }

    private static TextArea findCommentArea(VBox root) {
        for (Node child : root.getChildren()) {
            if (child instanceof VBox box) {
                for (Node nested : box.getChildren()) {
                    if (nested instanceof TextArea textArea) {
                        return textArea;
                    }
                }
            }
        }
        return null;
    }

    private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type) {
        return new MouseEvent(type, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false,
                false, false, false, null);
    }
}
