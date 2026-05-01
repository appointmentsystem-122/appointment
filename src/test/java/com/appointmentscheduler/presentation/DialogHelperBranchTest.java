package com.appointmentscheduler.presentation;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

/**
 * Covers {@link DialogHelper} branches that depend on a non-null {@link javafx.stage.Window} owner.
 */
class DialogHelperBranchTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void restoreAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void keyboardShortcuts_withOwnerWindow_noThrow() {
        System.setProperty("app.test.autoDialogs", "true");
        assertThatCode(() -> runOnFx(() -> {
            // Dialog.initOwner requires a Window whose scene is non-null (HeavyweightDialog copies stylesheets).
            Stage owner = new Stage();
            owner.setScene(new Scene(new StackPane(), 1, 1));
            DialogHelper.showKeyboardShortcutsClient(owner);
            DialogHelper.showKeyboardShortcutsAdmin(owner);
            return null;
        })).doesNotThrowAnyException();
    }

    @Test
    void keyboardShortcuts_nullOwner_autoDialogs_noThrow() {
        System.setProperty("app.test.autoDialogs", "true");
        assertThatCode(() -> runOnFx(() -> {
            DialogHelper.showKeyboardShortcutsClient(null);
            DialogHelper.showKeyboardShortcutsAdmin(null);
            return null;
        })).doesNotThrowAnyException();
    }

    @Test
    void keyboardShortcuts_nullOwner_nonAuto_skipsInitOwnerBranch() {
        System.clearProperty("app.test.autoDialogs");
        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                (alert, ctx) -> {
                    doReturn(FXCollections.observableArrayList()).when(alert).getButtonTypes();
                    doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait();
                })) {
            assertThatCode(() -> {
                DialogHelper.showKeyboardShortcutsClient(null);
                DialogHelper.showKeyboardShortcutsAdmin(null);
            }).doesNotThrowAnyException();
            assertThat(mocked.constructed()).hasSize(2);
        }
    }

    @Test
    void keyboardShortcuts_withOwner_nonAuto_callsInitOwner() {
        System.clearProperty("app.test.autoDialogs");
        assertThatCode(() -> runOnFx(() -> {
            try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                    (alert, ctx) -> {
                        doReturn(FXCollections.observableArrayList()).when(alert).getButtonTypes();
                        doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait();
                        doNothing().when(alert).initOwner(any());
                    })) {
                Stage owner = new Stage();
                owner.setScene(new Scene(new StackPane(), 1, 1));
                DialogHelper.showKeyboardShortcutsClient(owner);
                DialogHelper.showKeyboardShortcutsAdmin(owner);
                assertThat(mocked.constructed()).hasSize(2);
                verify(mocked.constructed().get(0)).initOwner(any());
                verify(mocked.constructed().get(1)).initOwner(any());
            }
            return null;
        })).doesNotThrowAnyException();
    }

    @Test
    void logoutAndConfirmation_nonAuto_resultBranches() {
        System.clearProperty("app.test.autoDialogs");

        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class, (alert, ctx) -> {
            if (ctx.getCount() == 1) {
                doReturn(Optional.empty()).when(alert).showAndWait();
            } else if (ctx.getCount() == 2) {
                doReturn(Optional.of(ButtonType.CANCEL)).when(alert).showAndWait();
            } else {
                doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait();
            }
        })) {
            boolean logout = DialogHelper.showLogoutConfirmation("App");
            boolean confirmCancel = DialogHelper.showConfirmation("t", "h", "c");
            boolean confirmOk = DialogHelper.showConfirmation("t", "h", "c");

            assertThat(logout).isFalse();
            assertThat(confirmCancel).isFalse();
            assertThat(confirmOk).isTrue();
        }
    }

    /**
     * Logout dialog uses a custom confirm {@link ButtonType}; {@link ButtonType#OK} is a different instance
     * with the same {@link javafx.scene.control.ButtonBar.ButtonData}, so reference equality must be false.
     */
    @Test
    void logoutConfirmation_presentButStockOkButton_returnsFalse() {
        System.clearProperty("app.test.autoDialogs");
        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class, (alert, ctx) ->
                doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait())) {
            assertThat(DialogHelper.showLogoutConfirmation("App")).isFalse();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void logoutConfirmation_userClicksDialogConfirm_returnsTrue() {
        System.clearProperty("app.test.autoDialogs");
        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                (alert, ctx) -> {
                    Optional<ButtonType> confirm = logoutConfirmButtonFromConstruction(ctx);
                    doReturn(FXCollections.observableArrayList()).when(alert).getButtonTypes();
                    doReturn(confirm.map(Optional::of).orElse(Optional.empty())).when(alert).showAndWait();
                })) {
            assertThat(DialogHelper.showLogoutConfirmation("App")).isTrue();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    /**
     * {@link Alert#Alert(Alert.AlertType, String, ButtonType...)} passes custom buttons as constructor args;
     * Mockito exposes them on {@link MockedConstruction.Context#arguments()} so we can return the same confirm
     * instance from {@code showAndWait()} (reference equality in {@link DialogHelper#showLogoutConfirmation}).
     */
    private static Optional<ButtonType> logoutConfirmButtonFromConstruction(MockedConstruction.Context ctx) {
        for (Object arg : ctx.arguments()) {
            if (arg instanceof ButtonType bt) {
                if (bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE
                        || bt.getButtonData() == ButtonType.CANCEL.getButtonData()) {
                    continue;
                }
                return Optional.of(bt);
            }
            if (arg instanceof ButtonType[] arr) {
                for (ButtonType bt : arr) {
                    if (bt.getButtonData() != ButtonBar.ButtonData.CANCEL_CLOSE
                            && bt.getButtonData() != ButtonType.CANCEL.getButtonData()) {
                        return Optional.of(bt);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Test
    void logoutConfirmation_autoDialogs_returnsTrueWithoutAlert() {
        System.setProperty("app.test.autoDialogs", "true");
        assertThatCode(() -> runOnFx(() -> {
            assertThat(DialogHelper.showLogoutConfirmation("App")).isTrue();
            return null;
        })).doesNotThrowAnyException();
    }

    @Test
    void infoAndError_nonAuto_callShowAndWait() {
        System.clearProperty("app.test.autoDialogs");
        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class, (alert, ctx) ->
                doReturn(Optional.of(ButtonType.OK)).when(alert).showAndWait())) {
            DialogHelper.showInfo("i", "info");
            DialogHelper.showError("e", "error");
            assertThat(mocked.constructed()).hasSize(2);
            verify(mocked.constructed().get(0)).showAndWait();
            verify(mocked.constructed().get(1)).showAndWait();
        }
    }

    /** {@link DialogHelper} applyDialogStyles catches failures from {@link Alert#getDialogPane()}. */
    @Test
    void applyDialogStyles_dialogPaneThrows_isSwallowed_autoDialogs() {
        System.setProperty("app.test.autoDialogs", "true");
        try (MockedConstruction<Alert> mocked = mockConstruction(Alert.class,
                (alert, ctx) -> doThrow(new RuntimeException("pane down")).when(alert).getDialogPane())) {
            assertThatCode(() -> {
                DialogHelper.showInfo("t", "c");
                DialogHelper.showError("e", "x");
                DialogHelper.showLogoutConfirmation("app");
                DialogHelper.showConfirmation("a", "b", "c");
            }).doesNotThrowAnyException();
            assertThat(mocked.constructed()).hasSize(4);
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
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
        return ref.get();
    }
}
