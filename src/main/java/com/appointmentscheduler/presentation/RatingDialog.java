package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Professional appointment rating dialog: 5 interactive stars, labels, optional comment.
 */
public final class RatingDialog {

    /**
     * Invokes the modal dialog wait; replaced in unit tests because {@link Dialog#showAndWait()} is
     * {@code final} and cannot be intercepted by mocking frameworks.
     */
    static Function<Dialog<RatingResult>, Optional<RatingResult>> ratingDialogBlockingShow = d -> d.showAndWait();

    static void resetRatingDialogBlockingShowForTests() {
        ratingDialogBlockingShow = d -> d.showAndWait();
    }

    private static final String STAR_FILLED = "\u2605";
    private static final String STAR_EMPTY = "\u2606";
    private static final String[] LABELS_AR = { "ضعيف", "مقبول", "جيد", "جيد جداً", "ممتاز" };
    private static final String[] LABELS_EN = { "Poor", "Fair", "Good", "Very Good", "Excellent" };

    /**
     * Result of rating: 1-5 stars and optional comment.
     */
    public static class RatingResult {
        private final int stars;
        private final String comment;

        public RatingResult(int stars, String comment) {
            this.stars = Math.max(1, Math.min(5, stars));
            this.comment = comment == null ? "" : comment.trim();
        }

        public int getStars() { return stars; }
        public String getComment() { return comment; }
    }

    /**
     * Shows the rating dialog and returns Optional of (stars, comment) if submitted.
     */
    /** First line of subtitle: formatted start time, or empty when the appointment has no slot. */
    static String ratingSubtitleDatePart(Appointment appointment) {
        if (appointment.getTimeSlot() == null) return "";
        return appointment.getTimeSlot().getStartTime().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy · HH:mm"));
    }

    /** Same rules as the dialog {@code setResultConverter} (OK vs cancel, default stars). */
    static RatingResult resultForDialogButton(ButtonType bt, int selectedStars, String commentText) {
        if (bt == ButtonType.OK) {
            int stars = selectedStars > 0 ? selectedStars : 1;
            return new RatingResult(stars, commentText);
        }
        return null;
    }

    public static Optional<RatingResult> show(Window owner, Appointment appointment) {
        if (appointment == null) return Optional.empty();

        Dialog<RatingResult> d = new Dialog<>();
        d.setTitle("تقييم الموعد / Rate your appointment");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        applyRatingOkButtonLabel((Button) d.getDialogPane().lookupButton(ButtonType.OK));

        VBox root = new VBox(20);
        root.getStyleClass().add("rating-dialog-content");
        root.setPadding(new Insets(8, 0, 0, 0));

        // Subtitle: date & type
        String dateStr = ratingSubtitleDatePart(appointment);
        String typeStr = ratingAppointmentTypeLabel(appointment);
        Label subtitle = new Label(dateStr + (typeStr.isEmpty() ? "" : " · " + typeStr));
        subtitle.getStyleClass().add("rating-subtitle");
        subtitle.setWrapText(true);
        root.getChildren().add(subtitle);

        // Stars row (clickable labels with hover)
        HBox starsBox = new HBox(6);
        starsBox.setAlignment(Pos.CENTER);
        starsBox.getStyleClass().add("rating-stars-box");

        final int[] selected = { 0 };
        final int[] hovered = { -1 };
        Label[] starLabels = new Label[5];
        Label hintLabel = new Label();
        hintLabel.getStyleClass().add("rating-hint");

        for (int i = 0; i < 5; i++) {
            final int idx = i + 1;
            Label star = new Label(STAR_EMPTY);
            star.getStyleClass().add("rating-star");
            star.setUserData(idx);
            star.setOnMouseEntered(e -> {
                hovered[0] = idx;
                ratingUpdateStarDisplay(starLabels, selected[0], hovered[0]);
                hintLabel.setText(LABELS_EN[idx - 1] + " / " + LABELS_AR[idx - 1]);
            });
            star.setOnMouseExited(e -> {
                hovered[0] = -1;
                ratingUpdateStarDisplay(starLabels, selected[0], hovered[0]);
                hintLabel.setText(selected[0] > 0 ? LABELS_EN[selected[0] - 1] + " / " + LABELS_AR[selected[0] - 1] : "");
            });
            star.setOnMouseClicked(e -> {
                selected[0] = idx;
                ratingUpdateStarDisplay(starLabels, selected[0], hovered[0]);
                hintLabel.setText(LABELS_EN[idx - 1] + " / " + LABELS_AR[idx - 1]);
            });
            starLabels[i] = star;
            starsBox.getChildren().add(star);
        }

        root.getChildren().add(starsBox);
        root.getChildren().add(hintLabel);

        // Optional comment
        Label commentLabel = new Label("تعليقك (اختياري) / Your comment (optional)");
        commentLabel.getStyleClass().add("rating-comment-label");
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Share more about your experience...");
        commentArea.getStyleClass().add("rating-comment-area");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);
        commentArea.setMaxHeight(80);
        VBox commentBox = new VBox(6, commentLabel, commentArea);
        root.getChildren().add(commentBox);

        d.getDialogPane().setContent(root);
        d.getDialogPane().getStyleClass().add("rating-dialog-pane");
        addRatingDialogStylesheet(d.getDialogPane());

        d.setResultConverter(bt -> resultForDialogButton(bt, selected[0], commentArea.getText()));

        if (owner != null) d.initOwner(owner);
        ratingUpdateStarDisplay(starLabels, 0, -1);
        if (DialogHelper.isAutoDialogs()) {
            // Simulate a "5 stars" submission to avoid blocking dialogs.
            int selectedStars = 5;
            ratingUpdateStarDisplay(starLabels, selectedStars, -1);
            hintLabel.setText(LABELS_EN[selectedStars - 1] + " / " + LABELS_AR[selectedStars - 1]);
            commentArea.setText("Auto feedback");
            return Optional.of(new RatingResult(selectedStars, commentArea.getText()));
        }

        return ratingDialogBlockingShow.apply(d);
    }

    static void addRatingDialogStylesheet(DialogPane pane) {
        addRatingDialogStylesheet(pane, () -> RatingDialog.class.getResource(
                "/com/appointmentscheduler/presentation/application-minimal.css"));
    }

    /**
     * Applies the rating dialog stylesheet; {@code urlSupplier} may throw (caught) for tests.
     */
    static void addRatingDialogStylesheet(DialogPane pane, Supplier<URL> urlSupplier) {
        try {
            URL url = urlSupplier.get();
            if (url != null) {
                pane.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    static void ratingUpdateStarDisplay(Label[] starLabels, int selected, int hover) {
        int show = hover >= 0 ? hover : selected;
        for (int i = 0; i < starLabels.length; i++) {
            starLabels[i].setText((i + 1) <= show ? STAR_FILLED : STAR_EMPTY);
            starLabels[i].getStyleClass().remove("rating-star-filled");
            if ((i + 1) <= show) starLabels[i].getStyleClass().add("rating-star-filled");
        }
    }

    /** Sets bilingual OK label when the dialog provides an OK button reference. */
    static void applyRatingOkButtonLabel(Button okButton) {
        if (okButton != null) okButton.setText("تأكيد التقييم / Submit");
    }

    static String ratingAppointmentTypeLabel(Appointment a) {
        if (a == null) return "";
        if (a instanceof com.appointmentscheduler.domain.AssessmentAppointment) return "First session";
        if (a instanceof com.appointmentscheduler.domain.FollowUpAppointment) return "Return visit";
        return a.getClass().getSimpleName().replace("Appointment", "");
    }
}
