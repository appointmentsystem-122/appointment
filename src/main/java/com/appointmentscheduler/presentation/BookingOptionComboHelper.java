package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.BookingOption;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * Consistent rendering of {@link BookingOption} in combo boxes (patient booking, standalone book screen).
 */
public final class BookingOptionComboHelper {

    private BookingOptionComboHelper() {}

    /** Display text for combo list/button cells; extracted for branch coverage without reflecting into {@link javafx.scene.control.Cell}. */
    static String listCellTextForBookingOption(BookingOption item, boolean empty) {
        return empty || item == null ? null : item.getDisplayLabel();
    }

    public static void configure(ComboBox<BookingOption> combo) {
        if (combo == null) return;
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BookingOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(listCellTextForBookingOption(item, empty));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BookingOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(listCellTextForBookingOption(item, empty));
            }
        });
    }
}
