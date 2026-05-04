package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.BookingRequestFields;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared UI setup for optional booking fields (patient dashboard + standalone book screen).
 */
public final class BookingExtrasUi {
    private static final Logger log = LoggerFactory.getLogger(BookingExtrasUi.class);

    private BookingExtrasUi() {}

    public static void configureReminderCombo(ComboBox<String> combo) {
        if (combo == null) return;
        if (!combo.getItems().isEmpty()) return;
        combo.getItems().setAll(
                BookingRequestFields.REMINDER_APP,
                BookingRequestFields.REMINDER_SMS,
                BookingRequestFields.REMINDER_NONE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String v) {
                return reminderChannelLabel(v);
            }

            @Override
            public String fromString(String s) {
                return BookingRequestFields.REMINDER_APP;
            }
        });
        combo.getSelectionModel().select(BookingRequestFields.REMINDER_APP);
    }

    public static void configureLanguageCombo(ComboBox<String> combo) {
        if (combo == null) return;
        if (!combo.getItems().isEmpty()) return;
        combo.getItems().setAll(
                BookingRequestFields.LANG_ANY,
                BookingRequestFields.LANG_AR,
                BookingRequestFields.LANG_EN);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String v) {
                return preferredLanguageLabel(v);
            }

            @Override
            public String fromString(String s) {
                return BookingRequestFields.LANG_ANY;
            }
        });
        combo.getSelectionModel().select(BookingRequestFields.LANG_ANY);
    }

    public static String reminderChannelLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case BookingRequestFields.REMINDER_EMAIL -> "Email / البريد الإلكتروني";
            case BookingRequestFields.REMINDER_SMS -> "SMS / رسالة نصية";
            case BookingRequestFields.REMINDER_NONE -> "None / بدون تذكير";
            default -> "App / إشعار التطبيق";
        };
    }

    public static String preferredLanguageLabel(String code) {
        if (code == null) return "";
        return switch (code) {
            case BookingRequestFields.LANG_AR -> "Arabic / العربية";
            case BookingRequestFields.LANG_EN -> "English / الإنجليزية";
            default -> "No preference / بدون تفضيل";
        };
    }

    public static void updatePartySpinner(Spinner<Integer> spinner, int maxParticipants) {
        if (spinner == null) return;
        int max = Math.max(1, maxParticipants);
        int current = 1;
        try {
            if (spinner.getValue() != null) {
                current = spinner.getValue();
            }
        } catch (RuntimeException ex) {
            log.debug("Could not read spinner value; using fallback", ex);
            current = 1;
        }
        int bounded = Math.min(Math.max(1, current), max);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, bounded));
        /* Keep control enabled so it never looks "broken"; only one value when max==1. */
        spinner.setDisable(false);
        spinner.setEditable(max > 1);
        if (max <= 1) {
            spinner.setTooltip(new Tooltip("هذا النوع محدود بشخص واحد · This type allows one guest only"));
        } else {
            spinner.setTooltip(null);
        }
    }
}
