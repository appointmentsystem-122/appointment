package com.appointmentscheduler.presentation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @AfterEach
    void restoreEnglish() {
        I18n.setLocale(Locale.ENGLISH);
    }

    @Test
    void get_emptyKey_returnsEmptyString() {
        assertThat(I18n.get("")).isEmpty();
    }

    @Test
    void getWithArgs_emptyKey_returnsEmptyWithoutFormatting() {
        assertThat(I18n.get("", "ignored")).isEmpty();
    }

    @Test
    void getAndLocale() {
        assertThat(I18n.get(null)).isEmpty();
        assertThat(I18n.get("error.title")).isNotBlank();
        I18n.setLanguage("ar");
        assertThat(I18n.getLocale().getLanguage()).isEqualTo("ar");
        I18n.setLanguage("en");
        assertThat(I18n.getLocale()).isEqualTo(Locale.ENGLISH);
        I18n.setLocale(Locale.ENGLISH);
        assertThat(I18n.get("missing.key.xyz")).isEqualTo("missing.key.xyz");
    }

    @Test
    void getWithArgs() {
        String t = I18n.get("error.title", "x");
        assertThat(t).isNotNull();
    }

    @Test
    void setLocale_nullIsNoOp() {
        Locale before = I18n.getLocale();
        I18n.setLocale(null);
        assertThat(I18n.getLocale()).isEqualTo(before);
    }

    @Test
    void setLanguage_emptyAndNull_noChange() {
        I18n.setLanguage("en");
        Locale before = I18n.getLocale();
        I18n.setLanguage("");
        assertThat(I18n.getLocale()).isEqualTo(before);
        I18n.setLanguage(null);
        assertThat(I18n.getLocale()).isEqualTo(before);
    }

    @Test
    void setLanguage_ar_SA_mapsToArabic() {
        I18n.setLanguage("ar_SA");
        assertThat(I18n.getLocale().getLanguage()).isEqualTo("ar");
        I18n.setLanguage("en");
    }

    @Test
    void setLanguage_arMixedCase_selectsArabic() {
        I18n.setLanguage("Ar");
        assertThat(I18n.getLocale().getLanguage()).isEqualTo("ar");
        I18n.setLanguage("EN");
        assertThat(I18n.getLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void setLanguage_nonArabicTag_defaultsToEnglish() {
        I18n.setLanguage("de");
        assertThat(I18n.getLocale()).isEqualTo(Locale.ENGLISH);
        I18n.setLanguage("fr");
        assertThat(I18n.getLocale()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void getWithArgs_nullArgs_returnsPattern() {
        String p = I18n.get("error.title", (Object[]) null);
        assertThat(p).isEqualTo(I18n.get("error.title"));
    }

    @Test
    void getWithArgs_emptyVarargs_returnsPatternWithoutMessageFormat() {
        String pattern = I18n.get("error.title");
        assertThat(I18n.get("error.title", new Object[0])).isEqualTo(pattern);
    }

    @Test
    void getWithArgs_messageFormatThrows_returnsPattern() {
        // Missing bundle key → pattern is the key; MessageFormat.format fails on this malformed pattern.
        assertThat(I18n.get("{0", "a")).isEqualTo("{0");
    }

    @Test
    void get_returnsKeyWhenBundleIsNull() throws Exception {
        Field bundleField = I18n.class.getDeclaredField("bundle");
        bundleField.setAccessible(true);
        ResourceBundle previous = (ResourceBundle) bundleField.get(null);
        try {
            bundleField.set(null, null);
            assertThat(I18n.get("login.email")).isEqualTo("login.email");
        } finally {
            bundleField.set(null, previous);
            I18n.setLocale(Locale.ENGLISH);
        }
    }
}
