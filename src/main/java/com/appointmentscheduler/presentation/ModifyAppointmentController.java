package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ScheduleService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Controller for the appointment-modification screen.
 * It loads an existing appointment, offers valid replacement slots, and submits the change request.
 */
public class ModifyAppointmentController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<TimeSlot> timeSlotCombo;

    @FXML
    private Label messageLabel;
    
    @FXML
    private Button btnSubmitModify;

    @FXML
    private Label currentApptLabel;

    private User currentUser;
    private Appointment targetAppointment;

    public static String appointmentIdToModify;

    /**
     * Resolves the appointment that should be edited and prepares the date and slot controls.
     */
    @FXML
    public void initialize() {
        currentUser = ApplicationContext.getAuthService().getCurrentUser();

        if (appointmentIdToModify != null) {
            targetAppointment = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments().stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getId() != null && a.getId().equals(appointmentIdToModify))
                .findFirst().orElse(null);
            appointmentIdToModify = null;
        } else {
            List<Appointment> all = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments();
            targetAppointment = all.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getPatient() != null && a.getPatient().getId().equals(currentUser.getId()))
                .filter(a -> "CONFIRMED".equals(a.getStatus()))
                .findFirst().orElse(null);
        }

        if (targetAppointment != null) {
            currentApptLabel.setText("Modifying: " + targetAppointment.getTimeSlot().toString());
        } else {
            currentApptLabel.setText("No confirmed appointment found to modify.");
        }

        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(true);
                    return;
                }
                setDisable(!isDateAllowedForModify(date));
            }
        });

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> onModifyDateChanged(newVal));
        timeSlotCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateModifyForm());
        validateModifyForm();

        Platform.runLater(() -> {
            if (messageLabel != null && messageLabel.getScene() != null) {
                messageLabel.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Q, javafx.scene.input.KeyCombination.CONTROL_DOWN),
                    this::handleLogout
                );
            }
        });
    }

    /** Current appointment's day stays selectable even if admin closed that day later. */
    private boolean isDateAllowedForModify(LocalDate date) {
        if (date == null) return false;
        if (date.isBefore(LocalDate.now())) return false;
        if (targetAppointment != null && targetAppointment.getTimeSlot() != null) {
            LocalDate cur = targetAppointment.getTimeSlot().getStartTime().toLocalDate();
            if (date.equals(cur)) return true;
        }
        return ApplicationContext.getScheduleService().isDateBookable(date);
    }

    private void onModifyDateChanged(LocalDate newVal) {
        if (newVal == null) {
            timeSlotCombo.getItems().clear();
            timeSlotCombo.setValue(null);
            showMessage("", false);
            validateModifyForm();
            return;
        }
        if (!isDateAllowedForModify(newVal)) {
            showMessage(BookingDateMessages.unavailable(newVal), true);
            timeSlotCombo.getItems().clear();
            timeSlotCombo.setValue(null);
            Platform.runLater(() -> {
                if (datePicker.getValue() != null && !isDateAllowedForModify(datePicker.getValue())) {
                    datePicker.setValue(null);
                }
            });
            validateModifyForm();
            return;
        }
        List<TimeSlot> available = ApplicationContext.getScheduleService().getAvailableSlots(newVal);
        timeSlotCombo.setItems(FXCollections.observableArrayList(available));
        if (available.isEmpty()) {
            showMessage(BookingDateMessages.unavailable(newVal), true);
        } else {
            showMessage("", false);
        }
        validateModifyForm();
    }
    
    private void validateModifyForm() {
        boolean isDateEmpty = datePicker.getValue() == null;
        boolean isSlotEmpty = timeSlotCombo.getValue() == null;

        if (isDateEmpty) datePicker.getStyleClass().add("error-input");
        else datePicker.getStyleClass().remove("error-input");
        
        if (isSlotEmpty) timeSlotCombo.getStyleClass().add("error-input");
        else timeSlotCombo.getStyleClass().remove("error-input");
        
        if (btnSubmitModify != null) {
            btnSubmitModify.setDisable(isDateEmpty || isSlotEmpty);
        }
    }

    /**
     * Reloads replacement slots for the newly selected date.
     */
    @FXML
    public void handleDateSelection() {
        onModifyDateChanged(datePicker.getValue());
    }

    /**
     * Attempts to update the selected appointment with the newly chosen slot.
     */
    @FXML
    public void handleModify() {
        if (targetAppointment == null) {
            showMessage("No appointment chosen to modify.", true);
            return;
        }

        LocalDate d = datePicker.getValue();
        if (d == null || !isDateAllowedForModify(d)) {
            showMessage(I18n.get("booking.day_unavailable_confirm"), true);
            return;
        }

        TimeSlot selectedSlot = timeSlotCombo.getValue();
        if (selectedSlot == null) {
            showMessage("Please select a new time slot.", true);
            return;
        }

        boolean success = ApplicationContext.getBookingService().modifyAppointment(targetAppointment.getId(), currentUser, selectedSlot);

        if (success) {
            showMessage("Appointment Time Modified Successfully!", false);
            handleBack();
        } else {
            showMessage("Failed to modify appointment (Might be ruled out or past).", true);
        }
    }

    /**
     * Returns to the appropriate dashboard without saving a modification.
     */
    @FXML
    public void handleBack() {
        if (currentUser != null && currentUser.isAdmin()) {
            MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard());
        } else {
            MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard());
        }
    }

    /**
     * Performs a confirmed logout from the modification screen.
     */
    @FXML
    public void handleLogout() {
        javafx.stage.Window owner = messageLabel != null && messageLabel.getScene() != null ? messageLabel.getScene().getWindow() : null;
        MainApp.performLogout(owner, currentUser);
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().setAll(isError ? "error-label" : "success-label");
    }
}
