package com.appointmentscheduler.presentation;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

class ErrorHandlerTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void restoreAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void logWarning_withoutThrowable_doesNotThrow() {
        assertThatCode(() -> ErrorHandler.logWarning("warn msg", null)).doesNotThrowAnyException();
    }

    @Test
    void logWarning_withThrowable_doesNotThrow() {
        assertThatCode(() -> ErrorHandler.logWarning("warn msg", new IOException("boom"))).doesNotThrowAnyException();
    }

    @Test
    void handle_withThrowable_andWithout_coversLoggingBranches() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            ErrorHandler.handle(null, "msg with cause", new IllegalStateException("x"));
            ErrorHandler.handle(null, "msg no cause", null);
            Platform.runLater(done::countDown);
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void showError_nullMessage_usesGeneric_andOwnerInitializes() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane(), 1, 1));
            ErrorHandler.showError(owner, null);
            ErrorHandler.showError(null, "explicit");
            Platform.runLater(done::countDown);
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void showError_nonAuto_invokesAlertShowAndWait() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                    (alert, ctx) -> doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait())) {
                ErrorHandler.showErrorOnFxThread(null, "user-visible");
                assertThat(mocked.constructed()).isNotEmpty();
                verify(mocked.constructed().get(0)).showAndWait();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
    }

    @Test
    void showErrorOnFxThread_withOwner_auto_skipsShowAndWait() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                    (alert, ctx) -> doNothing().when(alert).initOwner(any()))) {
                Stage owner = new Stage();
                owner.setScene(new Scene(new StackPane(), 40, 40));
                ErrorHandler.showErrorOnFxThread(owner, null);
                assertThat(mocked.constructed()).isNotEmpty();
                verify(mocked.constructed().get(0)).initOwner(any());
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void showErrorOnFxThread_withOwner_nonAuto_initOwnerAndShowAndWait() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                    (alert, ctx) -> {
                        doNothing().when(alert).initOwner(any());
                        doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait();
                    })) {
                Stage owner = new Stage();
                owner.setScene(new Scene(new StackPane(), 40, 40));
                ErrorHandler.showErrorOnFxThread(owner, "visible");
                verify(mocked.constructed().get(0)).initOwner(any());
                verify(mocked.constructed().get(0)).showAndWait();
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    }
}


