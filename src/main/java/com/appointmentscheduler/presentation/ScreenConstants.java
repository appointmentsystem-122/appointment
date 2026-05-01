package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppConfig;

/**
 * Central constants for screen navigation and titles.
 * Keeps FXML names and window titles in one place for consistency and easier changes.
 */
public final class ScreenConstants {

    private ScreenConstants() { }

    public static final String FXML_LOGIN = "Login.fxml";
    public static final String FXML_ADMIN_DASHBOARD = "AdminDashboard.fxml";
    public static final String FXML_PATIENT_DASHBOARD = "PatientDashboard.fxml";
    public static final String FXML_BOOK_APPOINTMENT = "BookAppointment.fxml";
    public static final String FXML_MODIFY_APPOINTMENT = "ModifyAppointment.fxml";

    public static final String BASE_PATH = "/com/appointmentscheduler/presentation/";

    public static String titleLogin() {
        return "Login - " + AppConfig.getAppName();
    }

    public static String titleAdminDashboard() {
        return "Admin - " + AppConfig.getAppName();
    }

    public static String titlePatientDashboard() {
        return "Client - " + AppConfig.getAppName();
    }

    public static String titleBookAppointment() {
        return "Book Appointment - " + AppConfig.getAppName();
    }

    public static String titleModifyAppointment() {
        return "Modify Appointment - " + AppConfig.getAppName();
    }
}
