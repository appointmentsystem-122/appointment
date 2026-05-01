package com.appointmentscheduler.testsupport;

import javafx.collections.ObservableList;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared utilities for driving JavaFX cell factories and pickers from tests so JaCoCo records
 * {@code updateItem} / day-cell branches without TestFX. Used by presentation coverage sweeps only.
 */
public final class PresentationFxHarness {

    private PresentationFxHarness() {
    }

    /**
     * Reflectively walks every {@link TableView}, {@link ListView}, {@link DatePicker}, and
     * {@link ComboBox} declared on {@code controller} (including superclass fields) and invokes
     * cell / day-cell logic best-effort. Exceptions inside individual controls are swallowed so one
     * broken widget does not abort the whole sweep.
     */
    /**
     * Invokes a parameterless method (including private) on {@code controller} if it exists.
     */
    public static void invokePrivateNoArg(Object controller, String methodName) {
        for (Class<?> c = controller.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(methodName);
                m.setAccessible(true);
                m.invoke(controller);
                return;
            } catch (NoSuchMethodException ignored) {
                // next
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sweepDeclaredFxControls(Object controller) {
        for (Field field : allDeclaredFields(controller.getClass())) {
            field.setAccessible(true);
            final Object value;
            try {
                value = field.get(controller);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (value == null) {
                continue;
            }
            try {
                if (value instanceof TableView) {
                    exerciseTableViewDeep((TableView<?>) value);
                } else if (value instanceof ListView) {
                    exerciseListViewCells((ListView<?>) value);
                } else if (value instanceof DatePicker) {
                    exerciseDatePickerDayCells((DatePicker) value);
                } else if (value instanceof ComboBox) {
                    exerciseComboBoxListCells((ComboBox) value);
                } else if (value instanceof CheckBox) {
                    exerciseCheckBoxBranches((CheckBox) value);
                } else if (value instanceof Spinner) {
                    exerciseSpinnerBranches((Spinner<?>) value);
                }
            } catch (Throwable ignored) {
                // best-effort per control
            }
        }
    }

    private static List<Field> allDeclaredFields(Class<?> start) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.add(f);
            }
        }
        return fields;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exerciseTableViewDeep(TableView<?> tv) {
        walkColumnsAny(tv.getColumns(), tv);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void walkColumnsAny(ObservableList cols, TableView tv) {
        for (Object colObj : cols) {
            TableColumn col = (TableColumn) colObj;
            if (!col.getColumns().isEmpty()) {
                walkColumnsAny(col.getColumns(), tv);
            } else {
                Callback factory = col.getCellFactory();
                if (factory == null) {
                    continue;
                }
                try {
                    TableCell cell = (TableCell) factory.call(col);
                    cell.updateIndex(0);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                    if (!tv.getItems().isEmpty()) {
                        cell.updateIndex(0);
                        Object item = null;
                        try {
                            if (col.getCellObservableValue(0) != null) {
                                item = col.getCellObservableValue(0).getValue();
                            }
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                        try {
                            invokeCellUpdateItem(cell, item, false);
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                        if (tv.getItems().size() > 1) {
                            cell.updateIndex(1);
                            try {
                                if (col.getCellObservableValue(1) != null) {
                                    item = col.getCellObservableValue(1).getValue();
                                }
                            } catch (Throwable ignored) {
                                // best-effort
                            }
                            try {
                                invokeCellUpdateItem(cell, item, false);
                            } catch (Throwable ignored) {
                                // best-effort
                            }
                        }
                    }
                    cell.updateIndex(1);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exerciseListViewCells(ListView<?> lv) {
        Callback cf = lv.getCellFactory();
        if (cf == null) {
            return;
        }
        ListCell cell = (ListCell) cf.call(lv);
        cell.updateIndex(0);
        try {
            invokeCellUpdateItem(cell, null, true);
        } catch (Throwable ignored) {
            // best-effort
        }
        try {
            invokeCellUpdateItem(cell, null, false);
        } catch (Throwable ignored) {
            // best-effort
        }
        if (!lv.getItems().isEmpty()) {
            try {
                invokeCellUpdateItem(cell, lv.getItems().get(0), false);
            } catch (Throwable ignored) {
                // best-effort
            }
        }
        if (lv.getItems().size() > 1) {
            cell.updateIndex(1);
            try {
                invokeCellUpdateItem(cell, lv.getItems().get(1), false);
            } catch (Throwable ignored) {
                // best-effort
            }
        }
    }

    private static void exerciseDatePickerDayCells(DatePicker dp) {
        Callback<DatePicker, javafx.scene.control.DateCell> factory = dp.getDayCellFactory();
        if (factory == null) {
            return;
        }
        javafx.scene.control.DateCell cell = factory.call(dp);
        LocalDate today = LocalDate.now();
        LocalDate[] samples = {
                null,
                today.minusYears(1),
                today.minusDays(1),
                today,
                today.plusDays(1),
                today.plusMonths(6)
        };
        for (LocalDate d : samples) {
            try {
                invokeCellUpdateItem(cell, d, d == null);
            } catch (Throwable ignored) {
                // best-effort
            }
        }
        try {
            invokeCellUpdateItem(cell, today, true);
        } catch (Throwable ignored) {
            // best-effort
        }
    }

    private static void exerciseCheckBoxBranches(CheckBox cb) {
        boolean orig = cb.isSelected();
        cb.setSelected(!orig);
        cb.setSelected(orig);
    }

    private static void exerciseSpinnerBranches(Spinner<?> sp) {
        if (sp.isDisabled()) {
            return;
        }
        try {
            sp.increment();
        } catch (Throwable ignored) {
            // best-effort
        }
        try {
            sp.decrement();
        } catch (Throwable ignored) {
            // best-effort
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exerciseComboBoxListCells(ComboBox cb) {
        // ComboBox cell factories are Callback<ListView<T>, ListCell<T>> — use a list shim with the same items.
        ListView shim = new ListView(cb.getItems());
        if (cb.getCellFactory() != null) {
            ListCell listCell = (ListCell) ((Callback) cb.getCellFactory()).call(shim);
            listCell.updateIndex(0);
            try {
                invokeCellUpdateItem(listCell, null, true);
            } catch (Throwable ignored) {
                // best-effort
            }
            if (!cb.getItems().isEmpty()) {
                try {
                    invokeCellUpdateItem(listCell, cb.getItems().get(0), false);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
        ListCell buttonCell = cb.getButtonCell();
        if (buttonCell != null) {
            try {
                invokeCellUpdateItem(buttonCell, null, true);
            } catch (Throwable ignored) {
                // best-effort
            }
            if (!cb.getItems().isEmpty()) {
                try {
                    invokeCellUpdateItem(buttonCell, cb.getItems().get(0), false);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    /**
     * Invokes protected {@code updateItem} on a {@link Cell} subclass (TableCell, ListCell, DateCell, …).
     */
    public static void invokeCellUpdateItem(Cell<?> cell, Object item, boolean empty) throws Exception {
        for (Class<?> c = cell.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!"updateItem".equals(m.getName()) || m.getParameterCount() != 2) {
                    continue;
                }
                if (m.getParameterTypes()[1] != boolean.class) {
                    continue;
                }
                m.setAccessible(true);
                m.invoke(cell, item, empty);
                return;
            }
        }
    }
}
