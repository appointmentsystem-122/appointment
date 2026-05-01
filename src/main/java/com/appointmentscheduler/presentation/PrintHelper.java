package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.presentation.notification.NotificationCenter;
import com.appointmentscheduler.presentation.notification.NotificationType;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;

public class PrintHelper {

    public static void printAppointmentReceipt(Appointment appt, javafx.stage.Window owner) {
        if (appt == null) return;
        
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            if (DialogHelper.isAutoDialogs()) {
                // Test seam: avoid opening OS print dialog during automated runs.
                NotificationCenter.getInstance().notify(
                        NotificationType.SUCCESS,
                        "Print",
                        "Appointment receipt sent to printer successfully."
                );
                ToastNotification.show(owner, NotificationType.SUCCESS, null,
                        "Appointment receipt sent to printer successfully.");
                return;
            }
            boolean proceed = job.showPrintDialog(owner);
            if (proceed) {
                // Construct a handsome VBox "receipt" view completely stripped of interactive UI
                VBox printNode = new VBox(20);
                printNode.setPadding(new Insets(50));
                printNode.setStyle("-fx-background-color: white;");
                
                Label header = new Label(com.appointmentscheduler.application.AppConfig.getBrandName() + " - Booking Record");
                header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: black;");
                
                Label patient = new Label("Customer: " + appt.getPatient().getName() + " (" + appt.getPatient().getEmail() + ")");
                patient.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
                
                Label date = new Label("Date & Time: " + appt.getTimeSlot().toString());
                date.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");
                
                Label type = new Label("Service Type: " + appt.getClass().getSimpleName());
                type.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
                
                Label status = new Label("Current Status: " + appt.getStatus());
                status.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
                
                Label footer = new Label("Please arrive a few minutes before your scheduled booking time.");
                footer.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");
                
                printNode.getChildren().addAll(header, patient, date, type, status, footer);
                
                boolean printed = job.printPage(printNode);
                if (printed) {
                    job.endJob();
                    NotificationCenter.getInstance().notify(NotificationType.SUCCESS, "Print", "Appointment receipt sent to printer successfully.");
                    ToastNotification.show(owner, NotificationType.SUCCESS, null, "Appointment receipt sent to printer successfully.");
                } else {
                    NotificationCenter.getInstance().notify(NotificationType.ERROR, "Print", "Printing failed.");
                    ToastNotification.show(owner, NotificationType.ERROR, null, "Printing failed.");
                }
            }
        } else {
            DialogHelper.showError("Printer Unavailable", "Could not initialize standard printing service.");
        }
    }
}
