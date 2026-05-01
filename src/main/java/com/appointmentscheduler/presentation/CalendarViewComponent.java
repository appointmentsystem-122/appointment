package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Calendar view with Daily, Weekly, or Monthly mode.
 */
public class CalendarViewComponent extends GridPane {

    public enum ViewMode { DAILY, WEEKLY, MONTHLY }

    private final ViewMode viewMode;
    private final LocalDate anchorDate;
    private final LocalDate currentWeekStart;
    private final int startHour = 8;
    private final int endHour = 18;

    public CalendarViewComponent(List<Appointment> appointments, LocalDate anchorDate) {
        this(appointments, anchorDate, ViewMode.WEEKLY);
    }

    public CalendarViewComponent(List<Appointment> appointments, LocalDate anchorDate, ViewMode viewMode) {
        this.viewMode = viewMode != null ? viewMode : ViewMode.WEEKLY;
        this.anchorDate = anchorDate != null ? anchorDate : LocalDate.now();
        this.currentWeekStart = this.anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        getStyleClass().add("calendar-grid");
        if (this.viewMode == ViewMode.DAILY) buildDailyGrid(); else if (this.viewMode == ViewMode.MONTHLY) buildMonthlyGrid(); else buildWeeklyGrid();
        if (this.viewMode == ViewMode.DAILY) populateDaily(appointments); else if (this.viewMode == ViewMode.MONTHLY) populateMonthly(appointments); else populateEvents(appointments);
    }

    private void buildWeeklyGrid() {
        // Headers (Days)
        for (int i = 0; i < 7; i++) {
            LocalDate day = currentWeekStart.plusDays(i);
            Label dayLabel = new Label(day.getDayOfWeek().name().substring(0, 3) + " " + day.format(DateTimeFormatter.ofPattern("MM/dd")));
            dayLabel.getStyleClass().add("calendar-header");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            add(dayLabel, i + 1, 0); // Col i+1, Row 0
            
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }
        
        // Time Column constraints
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(60);
        getColumnConstraints().add(0, timeCol);
        
        // Time Rows
        for (int hour = startHour; hour <= endHour; hour++) {
            int row = hour - startHour + 1;
            Label timeLabel = new Label(LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.getStyleClass().add("calendar-time-col");
            timeLabel.setMaxWidth(Double.MAX_VALUE);
            timeLabel.setMaxHeight(Double.MAX_VALUE);
            add(timeLabel, 0, row);
            
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(50); // Fixed height for 1 hour block
            rc.setVgrow(Priority.ALWAYS);
            getRowConstraints().add(rc);
            
            // Cells
            for (int col = 1; col <= 7; col++) {
                Pane cell = new Pane();
                cell.getStyleClass().add("calendar-cell");
                add(cell, col, row);
            }
        }
    }
    
    private void populateEvents(List<Appointment> appointments) {
        for (Appointment appt : appointments) {
            LocalDate apptDate = appt.getTimeSlot().getStartTime().toLocalDate();
            // Check if in current week
            if (!apptDate.isBefore(currentWeekStart) && !apptDate.isAfter(currentWeekStart.plusDays(6))) {
                
                int dayOffset = apptDate.getDayOfWeek().getValue() - 1; // MONDAY is 1 -> offset 0
                int col = dayOffset + 1;
                
                LocalTime startTime = appt.getTimeSlot().getStartTime().toLocalTime();
                int hour = startTime.getHour();
                if (hour >= startHour && hour <= endHour) {
                    int row = hour - startHour + 1;
                    
                    VBox eventBlock = new VBox();
                    eventBlock.getStyleClass().add("calendar-event");
                    applyAppointmentEventStyle(eventBlock, appt);

                    Label title = new Label(appt.getPatient().getName());
                    title.getStyleClass().add("calendar-event-title");
                    title.setMaxWidth(9999);
                    String typeName = appt.getClass().getSimpleName();
                    if (typeName.length() > 20) typeName = typeName.substring(0, 17) + "...";
                    Label sub = new Label(typeName);
                    sub.getStyleClass().add("calendar-event-sub");
                    eventBlock.getChildren().addAll(title, sub);
                    
                    // Tooltip
                    Tooltip tooltip = new Tooltip(
                        "Patient: " + appt.getPatient().getName() + "\n" +
                        "Time: " + appt.getTimeSlot().toString() + "\n" +
                        "Status: " + appt.getStatus() + "\n" +
                        "Type: " + appt.getClass().getSimpleName()
                    );
                    Tooltip.install(eventBlock, tooltip);
                    
                    // Add to grid, spanning 1 row (assuming 1 hour appt for visual simplicity)
                    add(eventBlock, col, row);
                    
                    // Margins for padding inside the cell
                    GridPane.setMargin(eventBlock, new Insets(2, 4, 2, 4));
                }
            }
        }
    }

    private void buildDailyGrid() {
        Label dayLabel = new Label(anchorDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));
        dayLabel.getStyleClass().add("calendar-header");
        add(dayLabel, 0, 0);
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(60);
        getColumnConstraints().add(timeCol);
        ColumnConstraints mainCol = new ColumnConstraints();
        mainCol.setPercentWidth(100);
        mainCol.setHgrow(Priority.ALWAYS);
        getColumnConstraints().add(mainCol);
        for (int hour = startHour; hour <= endHour; hour++) {
            int row = hour - startHour + 1;
            Label timeLabel = new Label(LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.getStyleClass().add("calendar-time-col");
            add(timeLabel, 0, row);
            Pane cell = new Pane();
            cell.getStyleClass().add("calendar-cell");
            add(cell, 1, row);
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(50);
            rc.setVgrow(Priority.ALWAYS);
            getRowConstraints().add(rc);
        }
    }

    private void populateDaily(List<Appointment> appointments) {
        for (Appointment appt : appointments) {
            if (!appt.getTimeSlot().getStartTime().toLocalDate().equals(anchorDate)) continue;
            LocalTime startTime = appt.getTimeSlot().getStartTime().toLocalTime();
            int hour = startTime.getHour();
            if (hour < startHour || hour > endHour) continue;
            int row = hour - startHour + 1;
            VBox eventBlock = new VBox();
            eventBlock.getStyleClass().add("calendar-event");
            styleEventBlock(eventBlock, appt);
            Label title = new Label(appt.getPatient().getName());
            title.getStyleClass().add("calendar-event-title");
            String typeName = appt.getClass().getSimpleName();
            if (typeName.length() > 18) typeName = typeName.substring(0, 15) + "...";
            Label sub = new Label(appt.getTimeSlot().getStartTime().toLocalTime() + " – " + typeName);
            sub.getStyleClass().add("calendar-event-sub");
            eventBlock.getChildren().addAll(title, sub);
            Tooltip.install(eventBlock, new Tooltip("Patient: " + appt.getPatient().getName() + "\nTime: " + appt.getTimeSlot() + "\nStatus: " + appt.getStatus()));
            add(eventBlock, 1, row);
            GridPane.setMargin(eventBlock, new Insets(2, 4, 2, 4));
        }
    }

    private void buildMonthlyGrid() {
        YearMonth ym = YearMonth.from(anchorDate);
        LocalDate first = ym.atDay(1);
        int startOffset = first.getDayOfWeek().getValue() - 1;
        Label monthLabel = new Label(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthLabel.getStyleClass().add("calendar-header");
        add(monthLabel, 0, 0, 7, 1);
        for (int i = 0; i < 7; i++) {
            Label d = new Label(DayOfWeek.of(i + 1).name().substring(0, 3));
            d.getStyleClass().add("calendar-header");
            add(d, i, 1);
        }
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }
        LocalDate d = first.minusDays(startOffset);
        for (int weekRow = 0; weekRow < 6; weekRow++) {
            for (int col = 0; col < 7; col++) {
                VBox cell = new VBox(2);
                cell.getStyleClass().add("calendar-cell");
                if (d.getMonth() != anchorDate.getMonth()) cell.setStyle("-fx-opacity: 0.5;");
                Label dayNum = new Label(String.valueOf(d.getDayOfMonth()));
                dayNum.getStyleClass().add("calendar-time-col");
                cell.getChildren().add(dayNum);
                add(cell, col, weekRow + 2);
                GridPane.setMargin(cell, new Insets(2));
                d = d.plusDays(1);
            }
            RowConstraints rc = new RowConstraints();
            rc.setPrefHeight(60);
            getRowConstraints().add(rc);
        }
    }

    private void populateMonthly(List<Appointment> appointments) {
        YearMonth ym = YearMonth.from(anchorDate);
        LocalDate first = ym.atDay(1);
        int startOffset = first.getDayOfWeek().getValue() - 1;
        for (Appointment appt : appointments) {
            LocalDate apptDate = appt.getTimeSlot().getStartTime().toLocalDate();
            if (apptDate.getMonth() != anchorDate.getMonth()) continue;
            int dayOfMonth = apptDate.getDayOfMonth();
            int gridPos = startOffset + dayOfMonth - 1;
            int col = gridPos % 7;
            int row = 2 + gridPos / 7;
            if (row > 7) continue;
            VBox eventBlock = new VBox();
            eventBlock.getStyleClass().add("calendar-event");
            styleEventBlock(eventBlock, appt);
            Label nameLbl = new Label(appt.getPatient().getName());
            nameLbl.getStyleClass().add("calendar-event-title");
            Label timeLbl = new Label(appt.getTimeSlot().getStartTime().toLocalTime().toString());
            timeLbl.getStyleClass().add("calendar-event-sub");
            eventBlock.getChildren().add(nameLbl);
            eventBlock.getChildren().add(timeLbl);
            add(eventBlock, col, row);
            GridPane.setMargin(eventBlock, new Insets(2));
        }
    }

    private void styleEventBlock(VBox eventBlock, Appointment appt) {
        applyAppointmentEventStyle(eventBlock, appt);
    }

    /**
     * Status / appointment-type styling for calendar chips (weekly grid + daily/monthly blocks).
     * Package-private for branch tests without building the full grid.
     */
    static void applyAppointmentEventStyle(VBox eventBlock, Appointment appt) {
        if (eventBlock == null || appt == null) {
            return;
        }
        String status = appt.getStatus();
        if ("CANCELLED".equals(status) || "EXPIRED".equals(status)) {
            eventBlock.setStyle("-fx-background-color: #94a3b8; -fx-opacity: 0.7;");
        } else if (appt.getClass().getSimpleName().contains("Urgent")) {
            eventBlock.setStyle("-fx-background-color: linear-gradient(to bottom right, #dc2626, #b91c1c);");
        } else if (appt.getClass().getSimpleName().contains("Assessment")) {
            eventBlock.setStyle("-fx-background-color: linear-gradient(to bottom right, #d97706, #b45309);");
        } else if ("CONFIRMED".equals(status)) {
            eventBlock.setStyle("-fx-background-color: linear-gradient(to bottom right, #059669, #047857);");
        }
    }
}
