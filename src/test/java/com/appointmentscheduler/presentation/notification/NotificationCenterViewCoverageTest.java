package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationCenterViewCoverageTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void install_and_bell_action_paths_execute_without_errors() {
        NotificationCenter center = NotificationCenter.getInstance();

        runOnFxVoid(center::clear);
        waitFxDrain();
        runOnFxVoid(() -> {
            center.notify(NotificationType.INFO, "t1", "m1");
            center.notify(NotificationType.SUCCESS, "t2", "m2");
        });
        waitFxDrain();

        HBox container = runOnFx(HBox::new);
        Stage stage = runOnFx(Stage::new);
        runOnFxVoid(() -> {
            Scene scene = new Scene(new StackPane(container), 800, 400);
            stage.setScene(scene);
            stage.show();
        });

        assertThatCode(() -> runOnFxVoid(() -> NotificationCenterView.install(center, container)))
                .doesNotThrowAnyException();

        runOnFxVoid(() -> {
            assertThat(container.getChildren()).isNotEmpty();
            StackPane bellPane = (StackPane) container.getChildren().get(0);
            Button bell = (Button) bellPane.getChildren().get(0);
            bell.fire();
        });
    }

    @Test
    void install_nullArguments_returnsEarly() {
        NotificationCenter center = NotificationCenter.getInstance();
        HBox box = new HBox();
        assertThatCode(() -> runOnFxVoid(() -> {
            NotificationCenterView.install(null, box);
            NotificationCenterView.install(center, null);
        })).doesNotThrowAnyException();
    }

    @Test
    void bell_withoutScene_doesNotThrow() {
        NotificationCenter center = NotificationCenter.getInstance();
        runOnFxVoid(center::clear);
        waitFxDrain();
        HBox container = new HBox();
        runOnFxVoid(() -> NotificationCenterView.install(center, container));
        runOnFxVoid(() -> {
            StackPane bellPane = (StackPane) container.getChildren().get(0);
            Button bell = (Button) bellPane.getChildren().get(0);
            bell.fire();
        });
    }

    @Test
    void popover_actions_and_mouseBranches() {
        NotificationCenter center = NotificationCenter.getInstance();
        runOnFxVoid(center::clear);
        waitFxDrain();
        runOnFxVoid(() -> center.notify(NotificationType.INFO, "click", "msg"));
        waitFxDrain();

        HBox container = runOnFx(HBox::new);
        Stage stage = runOnFx(Stage::new);
        runOnFxVoid(() -> {
            Scene scene = new Scene(new StackPane(container), 800, 400);
            stage.setScene(scene);
            stage.show();
        });

        runOnFxVoid(() -> NotificationCenterView.install(center, container));
        runOnFxVoid(() -> {
            StackPane bellPane = (StackPane) container.getChildren().get(0);
            Button bell = (Button) bellPane.getChildren().get(0);
            bell.fire();
        });
        waitFxDrain();

        runOnFxVoid(() -> {
            Popup popup = findPopup();
            assertThat(popup).isNotNull();
            VBox root = (VBox) popup.getContent().get(0);
            @SuppressWarnings("unchecked")
            ListView<AppNotification> list = (ListView<AppNotification>) root.getChildren().get(1);
            assertThat(list.getItems()).isNotEmpty();
            list.getSelectionModel().select(0);

            MouseEvent secondary = new MouseEvent(
                    MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0,
                    MouseButton.SECONDARY,
                    1,
                    false, false, false, false,
                    false, false, false,
                    false, false, false,
                    null);
            list.getOnMouseClicked().handle(secondary);

            MouseEvent primary = new MouseEvent(
                    MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0,
                    MouseButton.PRIMARY,
                    1,
                    false, false, false, false,
                    false, false, false,
                    false, false, false,
                    null);
            list.getOnMouseClicked().handle(primary);

            HBox actions = (HBox) root.getChildren().get(2);
            Button markAll = (Button) actions.getChildren().get(0);
            Button clear = (Button) actions.getChildren().get(1);
            markAll.fire();
            clear.fire();
            popup.hide();
        });
    }

    private static Popup findPopup() {
        for (Window w : Window.getWindows()) {
            if (w instanceof Popup) {
                return (Popup) w;
            }
        }
        return null;
    }

    private static void waitFxDrain() {
        runOnFxVoid(() -> {
            // no-op: waits until queued FX tasks complete
        });
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
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("FX task timed out");
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
