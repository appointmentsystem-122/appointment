package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.BookingAppointmentFactory;
import com.appointmentscheduler.application.BookingCatalog;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.BookingFailureCodes;
import com.appointmentscheduler.application.BookingFormValidator;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.ScheduleService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookAppointmentController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private ComboBox<TimeSlot> timeSlotCombo;

    @FXML
    private ComboBox<BookingOption> typeCombo;

    @FXML
    private Label messageLabel;
    
    @FXML
    private Button btnConfirmBooking;

    @FXML
    private TextArea notesField;
    @FXML
    private TextField contactPhoneField;
    @FXML
    private TextField accessibilityField;
    @FXML
    private ComboBox<String> reminderCombo;
    @FXML
    private ComboBox<String> languageCombo;
    @FXML
    private Spinner<Integer> partySizeSpinner;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = ApplicationContext.getAuthService().getCurrentUser();

        typeCombo.getItems().setAll(BookingCatalog.listOptions());
        BookingOptionComboHelper.configure(typeCombo);
        if (!typeCombo.getItems().isEmpty()) {
            typeCombo.getSelectionModel().selectFirst();
        }

        BookingExtrasUi.configureReminderCombo(reminderCombo);
        BookingExtrasUi.configureLanguageCombo(languageCombo);
        refreshPartySpinnerForType();

        applyBookScreenDayCells();

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> onBookingDateChanged(newVal));
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshPartySpinnerForType();
            applyBookScreenDayCells();
            validateBookingForm();
            LocalDate dv = datePicker.getValue();
            if (dv != null) {
                ScheduleService ss = ApplicationContext.getScheduleService();
                int dur = newVal != null ? newVal.getDurationMinutes() : 60;
                if (ss != null && ss.isDateBookable(dv, dur)) {
                    loadTimeSlotsAsync(dv);
                } else {
                    onBookingDateChanged(dv);
                }
            }
        });
        timeSlotCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateBookingForm());

        if (notesField != null) {
            notesField.textProperty().addListener((o, a, b) -> {
                if (b != null && b.length() > 1000) {
                    notesField.setText(b.substring(0, 1000));
                }
            });
        }

        validateBookingForm();

        Platform.runLater(() -> {
            if (messageLabel != null && messageLabel.getScene() != null) {
                messageLabel.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Q, javafx.scene.input.KeyCombination.CONTROL_DOWN),
                    this::handleLogout
                );
            }
        });
    }

    private int selectedDurationMinutes() {
        return typeCombo.getValue() != null ? typeCombo.getValue().getDurationMinutes() : 60;
    }

    private void refreshPartySpinnerForType() {
        BookingOption opt = typeCombo.getValue();
        int max = opt != null ? opt.getMaxParticipants() : 10;
        BookingExtrasUi.updatePartySpinner(partySizeSpinner, max);
    }

    private void applyBookScreenDayCells() {
        ScheduleService schedule = ApplicationContext.getScheduleService();
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(true);
                    return;
                }
                int dur = typeCombo.getValue() != null ? typeCombo.getValue().getDurationMinutes() : 60;
                boolean bookable = schedule.isDateBookable(date, dur);
                setDisable(!bookable);
            }
        });
    }
    
    private void onBookingDateChanged(LocalDate newVal) {
        if (newVal == null) {
            timeSlotCombo.getItems().clear();
            timeSlotCombo.setValue(null);
            showMessage("", false);
            validateBookingForm();
            return;
        }
        ScheduleService schedule = ApplicationContext.getScheduleService();
        int dur = selectedDurationMinutes();
        if (!schedule.isDateBookable(newVal, dur)) {
            showMessage(BookingDateMessages.unavailable(newVal), true);
            timeSlotCombo.getItems().clear();
            timeSlotCombo.setValue(null);
            Platform.runLater(() -> {
                if (datePicker.getValue() != null && !schedule.isDateBookable(datePicker.getValue(), selectedDurationMinutes())) {
                    datePicker.setValue(null);
                }
            });
            validateBookingForm();
            return;
        }
        loadTimeSlotsAsync(newVal);
        validateBookingForm();
    }
    
    private void validateBookingForm() {
        boolean blockedOpen = currentUser != null && ApplicationContext.getBookingService() != null
                && ApplicationContext.getBookingService().patientHasBlockingOpenAppointment(currentUser.getId());
        BookingFormValidator.Result state = BookingFormValidator.evaluate(
                datePicker.getValue() == null,
                typeCombo.getValue() == null,
                timeSlotCombo.getValue() == null,
                blockedOpen);

        if (state.dateMissing()) datePicker.getStyleClass().add("error-input");
        else datePicker.getStyleClass().remove("error-input");
        
        if (state.typeMissing()) typeCombo.getStyleClass().add("error-input");
        else typeCombo.getStyleClass().remove("error-input");

        if (btnConfirmBooking != null) {
            btnConfirmBooking.setDisable(!state.canSubmit());
        }
    }

    @FXML
    public void handleDateSelection() {
        onBookingDateChanged(datePicker.getValue());
    }

    private void loadTimeSlotsAsync(LocalDate selectedDate) {
        final int dur = selectedDurationMinutes();
        showLoadingState(true);
        javafx.concurrent.Task<List<TimeSlot>> loadSlotsTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<TimeSlot> call() {
                return ApplicationContext.getScheduleService().getAvailableSlots(selectedDate, dur);
            }

            @Override
            protected void succeeded() {
                List<TimeSlot> available = getValue();
                timeSlotCombo.setItems(FXCollections.observableArrayList(available));
                if (available.isEmpty()) {
                    showMessage(BookingDateMessages.unavailable(selectedDate), true);
                } else {
                    showMessage("", false);
                }
                showLoadingState(false);
            }

            @Override
            protected void failed() {
                showLoadingState(false);
                DialogHelper.showError(
                    I18n.get("error.title"),
                    I18n.get("error.generic")
                );
            }
        };
        new Thread(loadSlotsTask).start();
    }

    @FXML
    public void handleBook() {
        BookingOption opt = typeCombo.getValue();
        LocalDate d = datePicker.getValue();
        ScheduleService schedule = ApplicationContext.getScheduleService();
        if (opt == null || d == null || !schedule.isDateBookable(d, opt.getDurationMinutes())) {
            showMessage(I18n.get("booking.day_unavailable_confirm"), true);
            return;
        }

        TimeSlot selectedSlot = timeSlotCombo.getValue();

        if (selectedSlot == null) {
            showMessage(I18n.get("booking.error.message"), true);
            return;
        }

        if (currentUser != null && ApplicationContext.getBookingService() != null
                && ApplicationContext.getBookingService().patientHasBlockingOpenAppointment(currentUser.getId())) {
            showMessage(I18n.get("booking.blocked.completion_required.detail"), true);
            return;
        }

        Appointment newAppointment = BookingAppointmentFactory.create(opt, currentUser, selectedSlot);
        int party = 1;
        if (partySizeSpinner != null && !partySizeSpinner.isDisabled() && partySizeSpinner.getValue() != null) {
            party = partySizeSpinner.getValue();
        }
        BookingRequestFields.applyTo(
                newAppointment,
                notesField != null ? notesField.getText() : null,
                contactPhoneField != null ? contactPhoneField.getText() : null,
                reminderCombo != null ? reminderCombo.getValue() : null,
                accessibilityField != null ? accessibilityField.getText() : null,
                languageCombo != null ? languageCombo.getValue() : null,
                party,
                opt.getMaxParticipants());

        Optional<String> fail = ApplicationContext.getBookingService().tryBookWithReason(newAppointment, null);

        if (fail.isEmpty()) {
            showMessage(I18n.get("booking.success.message"), false);
            handleBack();
        } else if (BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED.equals(fail.get())) {
            showMessage(I18n.get("booking.blocked.completion_required.detail"), true);
        } else {
            showMessage(I18n.get("booking.error.message") + "\n\n" + fail.get(), true);
        }
    }

    @FXML
    public void handleBack() {
        if (currentUser != null && currentUser.isAdmin()) {
            MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard());
        } else {
            MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard());
        }
    }

    @FXML
    public void handleLogout() {
        javafx.stage.Window owner = messageLabel != null && messageLabel.getScene() != null ? messageLabel.getScene().getWindow() : null;
        MainApp.performLogout(owner, currentUser);
    }

    @FXML
    public void handleConfirmBooking() {
        handleBook();
    }

    private void showMessage(String msg, boolean isError) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().setAll(isError ? "error-label" : "success-label");
    }

    private void showLoadingState(boolean loading) {
        if (loading) {
            messageLabel.setText(I18n.get("loading"));
            messageLabel.getStyleClass().setAll("info-label");
            timeSlotCombo.setDisable(true);
            if (btnConfirmBooking != null) btnConfirmBooking.setDisable(true);
        } else {
            timeSlotCombo.setDisable(false);
            validateBookingForm();
        }
    }
}
