package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PatientInboxServiceTest {

    @Test
    void append_skipsNullUserIdOrNullEntry() {
        PatientInboxService s = new PatientInboxService();
        s.append(null, entry("t"));
        s.append("u1", null);
        assertThat(s.listRecent("u1", 10)).isEmpty();
    }

    @Test
    void listRecent_nullUserIdReturnsEmpty() {
        PatientInboxService s = new PatientInboxService();
        s.append("u1", entry("a"));
        assertThat(s.listRecent(null, 10)).isEmpty();
    }

    @Test
    void listRecent_unknownUserReturnsEmpty() {
        assertThat(new PatientInboxService().listRecent("missing", 5)).isEmpty();
    }

    @Test
    void listRecent_reversedAndTruncatesToMax() {
        PatientInboxService s = new PatientInboxService();
        s.append("u1", entry("a"));
        s.append("u1", entry("b"));
        assertThat(s.listRecent("u1", 1)).hasSize(1);
        assertThat(s.listRecent("u1", 1).get(0).getTitle()).isEqualTo("b");
        assertThat(s.listRecent("u1", 10)).hasSize(2);
        assertThat(s.listRecent("u1", 10).get(0).getTitle()).isEqualTo("b");
    }

    @Test
    void evictsOldestWhenOverPerUserCap() {
        PatientInboxService s = new PatientInboxService();
        for (int i = 0; i < 102; i++) {
            s.append("u1", entry("m" + i));
        }
        assertThat(s.listRecent("u1", 5)).hasSize(5);
        assertThat(s.listRecent("u1", 5).get(0).getTitle()).isEqualTo("m101");
    }

    private static PatientInboxEntry entry(String title) {
        return new PatientInboxEntry(title, "body", LocalDateTime.now(), "Org");
    }
}
