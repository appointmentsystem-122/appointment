package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingExtrasUiTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void configureReminderCombo_null_noOp() {
        assertThatCode(() -> BookingExtrasUi.configureReminderCombo(null)).doesNotThrowAnyException();
    }

    @Test
    void configureLanguageCombo_null_noOp() {
        assertThatCode(() -> BookingExtrasUi.configureLanguageCombo(null)).doesNotThrowAnyException();
    }

    @Test
    void configureCombos_stringConverters_fromStringBranches() {
        ComboBox<String> reminder = new ComboBox<>();
        BookingExtrasUi.configureReminderCombo(reminder);
        assertThat(reminder.getConverter().fromString("anything")).isEqualTo(BookingRequestFields.REMINDER_APP);

        ComboBox<String> lang = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(lang);
        assertThat(lang.getConverter().fromString("anything")).isEqualTo(BookingRequestFields.LANG_ANY);
    }

    @Test
    void configureReminderCombo_skipsWhenItemsAlreadyPresent() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add("existing");
        BookingExtrasUi.configureReminderCombo(combo);
        assertThat(combo.getItems()).containsExactly("existing");
    }

    @Test
    void configureReminderCombo_populatesAndSelectsApp() {
        ComboBox<String> combo = new ComboBox<>();
        BookingExtrasUi.configureReminderCombo(combo);
        assertThat(combo.getItems()).contains(
                BookingRequestFields.REMINDER_APP,
                BookingRequestFields.REMINDER_SMS,
                BookingRequestFields.REMINDER_NONE);
        assertThat(combo.getValue()).isEqualTo(BookingRequestFields.REMINDER_APP);
    }

    @Test
    void configureLanguageCombo_populatesAndSelectsAny() {
        ComboBox<String> combo = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(combo);
        assertThat(combo.getItems()).contains(
                BookingRequestFields.LANG_ANY,
                BookingRequestFields.LANG_AR,
                BookingRequestFields.LANG_EN);
        assertThat(combo.getValue()).isEqualTo(BookingRequestFields.LANG_ANY);
    }

    @Test
    void configureLanguageCombo_skipsWhenItemsAlreadyPresent() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add("preset");
        BookingExtrasUi.configureLanguageCombo(combo);
        assertThat(combo.getItems()).containsExactly("preset");
    }

    @Test
    void reminderChannelLabel_coversBranches() {
        assertThat(BookingExtrasUi.reminderChannelLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_EMAIL))
                .contains("Email");
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_SMS))
                .contains("SMS");
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_NONE))
                .contains("بدون");
        assertThat(BookingExtrasUi.reminderChannelLabel(BookingRequestFields.REMINDER_APP))
                .contains("إشعار");
        assertThat(BookingExtrasUi.reminderChannelLabel("UNKNOWN")).contains("إشعار");
    }

    @Test
    void preferredLanguageLabel_coversBranches() {
        assertThat(BookingExtrasUi.preferredLanguageLabel(null)).isEmpty();
        assertThat(BookingExtrasUi.preferredLanguageLabel(BookingRequestFields.LANG_AR)).contains("العربية");
        assertThat(BookingExtrasUi.preferredLanguageLabel(BookingRequestFields.LANG_EN)).contains("English");
        assertThat(BookingExtrasUi.preferredLanguageLabel(BookingRequestFields.LANG_ANY)).contains("بدون تفضيل");
    }

    @Test
    void updatePartySpinner_null_noOp() {
        assertThatCode(() -> BookingExtrasUi.updatePartySpinner(null, 3)).doesNotThrowAnyException();
    }

    @Test
    void updatePartySpinner_maxOne_disablesEditingAndSetsTooltip() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        BookingExtrasUi.updatePartySpinner(spinner, 1);
        assertThat(spinner.getValue()).isEqualTo(1);
        assertThat(spinner.isEditable()).isFalse();
        assertThat(spinner.getTooltip()).isNotNull();
    }

    @Test
    void updatePartySpinner_maxGreaterThanOne_editable() {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));
        BookingExtrasUi.updatePartySpinner(spinner, 5);
        assertThat(spinner.getValue()).isBetween(1, 5);
        assertThat(spinner.isEditable()).isTrue();
        assertThat(spinner.getTooltip()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePartySpinner_getValueThrows_fallsBackToOne() {
        Spinner<Integer> spinner = mock(Spinner.class);
        when(spinner.getValue()).thenThrow(new RuntimeException("unstable value"));
        assertThatCode(() -> BookingExtrasUi.updatePartySpinner(spinner, 3)).doesNotThrowAnyException();
        Mockito.verify(spinner).setValueFactory(Mockito.any(SpinnerValueFactory.class));
    }

    @Test
    void configureReminderCombo_converterToString_coversLabelSwitch() {
        ComboBox<String> combo = new ComboBox<>();
        BookingExtrasUi.configureReminderCombo(combo);
        var c = combo.getConverter();
        assertThat(c.toString(BookingRequestFields.REMINDER_APP)).isNotBlank();
        assertThat(c.toString(BookingRequestFields.REMINDER_SMS)).contains("SMS");
        assertThat(c.toString(BookingRequestFields.REMINDER_NONE)).contains("بدون");
    }

    @Test
    void configureLanguageCombo_converterToString_coversLabelSwitch() {
        ComboBox<String> combo = new ComboBox<>();
        BookingExtrasUi.configureLanguageCombo(combo);
        var c = combo.getConverter();
        assertThat(c.toString(BookingRequestFields.LANG_AR)).contains("العربية");
        assertThat(c.toString(BookingRequestFields.LANG_EN)).contains("English");
        assertThat(c.toString(BookingRequestFields.LANG_ANY)).contains("بدون تفضيل");
    }
}
