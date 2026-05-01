package com.appointmentscheduler.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Executive dashboard KPIs with threshold status (OK / WARNING / CRITICAL).
 */
public final class ExecutiveKpis {

    public enum Status { OK, WARNING, CRITICAL }

    public static final class KpiRow {
        public final String label;
        public final String value;
        public final Status status;
        public final String thresholdInfo;

        public KpiRow(String label, String value, Status status, String thresholdInfo) {
            this.label = label;
            this.value = value;
            this.status = status;
            this.thresholdInfo = thresholdInfo != null ? thresholdInfo : "";
        }
    }

    private static final double CANCELLATION_WARNING_PCT = 15.0;
    private static final double CANCELLATION_CRITICAL_PCT = 30.0;

    /**
     * Builds executive KPI list with status for the given reporting service and optional clinic.
     */
    public static List<KpiRow> build(ReportingService reportingService, String clinicId) {
        List<KpiRow> rows = new ArrayList<>();
        long total = reportingService.getTotalAppointmentsCount(clinicId);
        long today = reportingService.getTodayAppointmentsCount(clinicId);
        double cancelRate = reportingService.getCancellationRate(clinicId);
        int peak = reportingService.getPeakBookingHour();

        rows.add(new KpiRow("إجمالي الحجوزات / Total bookings", String.valueOf(total), Status.OK, ""));
        rows.add(new KpiRow("حجوزات اليوم / Today's bookings", String.valueOf(today), Status.OK, ""));
        Status cancelStatus = cancelRate >= CANCELLATION_CRITICAL_PCT ? Status.CRITICAL
                : cancelRate >= CANCELLATION_WARNING_PCT ? Status.WARNING : Status.OK;
        rows.add(new KpiRow("نسبة الإلغاء / Cancellation rate", String.format("%.1f%%", cancelRate), cancelStatus,
                "تحذير >" + CANCELLATION_WARNING_PCT + "% · حرج >" + CANCELLATION_CRITICAL_PCT + "% · Warning >"
                        + CANCELLATION_WARNING_PCT + "%, Critical >" + CANCELLATION_CRITICAL_PCT + "%"));
        rows.add(new KpiRow("ساعة الذروة / Peak hour", String.format("%02d:00", peak), Status.OK, ""));
        return rows;
    }
}
