package com.appointmentscheduler.presentation;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

/**
 * Utility for applying a blocking loading spinner overlay to a specific layout region.
 */
public class LoadingSpinnerOverlay {

    private final StackPane overlay;

    public LoadingSpinnerOverlay() {
        overlay = new StackPane();
        overlay.getStyleClass().add("progress-overlay");
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("progress-indicator");
        spinner.setMaxSize(50, 50);
        
        overlay.getChildren().add(spinner);
        StackPane.setAlignment(spinner, Pos.CENTER);
        
        overlay.setVisible(false); // Hidden by default
    }
    
    public void attachTo(StackPane container) {
        if (!container.getChildren().contains(overlay)) {
            container.getChildren().add(overlay);
        }
    }
    
    public void show() {
        overlay.setVisible(true);
        overlay.toFront();
    }
    
    public void hide() {
        overlay.setVisible(false);
    }
}
