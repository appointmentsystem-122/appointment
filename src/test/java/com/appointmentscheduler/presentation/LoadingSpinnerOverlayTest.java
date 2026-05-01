package com.appointmentscheduler.presentation;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LoadingSpinnerOverlayTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void attachShowHide_andDoubleAttach() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                StackPane root = new StackPane();
                LoadingSpinnerOverlay overlay = new LoadingSpinnerOverlay();
                overlay.attachTo(root);
                assertThat(root.getChildren()).hasSize(1);
                overlay.attachTo(root);
                assertThat(root.getChildren()).hasSize(1);
                overlay.show();
                overlay.hide();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}
