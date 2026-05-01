package com.appointmentscheduler.presentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvUtilsTest {

    @Test
    void null_becomesEmptyQuoted() {
        assertThat(CsvUtils.escape(null)).isEqualTo("\"\"");
    }

    @Test
    void plainValue_wrappedInQuotes() {
        assertThat(CsvUtils.escape("hello")).isEqualTo("\"hello\"");
    }

    @Test
    void innerQuotes_doubledPerRfc4180() {
        assertThat(CsvUtils.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    @Test
    void nonString_toStringWrapped() {
        assertThat(CsvUtils.escape(42)).isEqualTo("\"42\"");
        assertThat(CsvUtils.escape(Boolean.TRUE)).isEqualTo("\"true\"");
    }

    @Test
    void newlinesPreservedInQuotes() {
        assertThat(CsvUtils.escape("a\nb")).isEqualTo("\"a\nb\"");
    }
}
