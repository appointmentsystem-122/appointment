package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaffInboxServiceTest {

    @Test
    void append_nullIgnored() {
        StaffInboxService s = new StaffInboxService();
        s.append(null);
        assertThat(s.listRecent(10)).isEmpty();
    }

    @Test
    void listRecent_nonPositiveReturnsEmpty() {
        StaffInboxService s = new StaffInboxService();
        s.append(msg("a"));
        assertThat(s.listRecent(0)).isEmpty();
        assertThat(s.listRecent(-1)).isEmpty();
    }

    @Test
    void listRecent_reversedNewestFirst_andRespectsMax() {
        StaffInboxService s = new StaffInboxService();
        s.append(msg("first"));
        s.append(msg("second"));
        List<StaffContactMessage> all = s.listRecent(10);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getSubject()).isEqualTo("second");
        assertThat(all.get(1).getSubject()).isEqualTo("first");

        List<StaffContactMessage> one = s.listRecent(1);
        assertThat(one).hasSize(1);
        assertThat(one.get(0).getSubject()).isEqualTo("second");
    }

    @Test
    void evictsOldestWhenOverGlobalCap() {
        StaffInboxService s = new StaffInboxService();
        for (int i = 0; i < 502; i++) {
            s.append(new StaffContactMessage("s" + i, "b", LocalDateTime.now(), "id", "n", "e"));
        }
        List<StaffContactMessage> recent = s.listRecent(5);
        assertThat(recent.get(0).getSubject()).isEqualTo("s501");
        assertThat(recent.get(4).getSubject()).isEqualTo("s497");
    }

    private static StaffContactMessage msg(String subject) {
        return new StaffContactMessage(subject, "body", LocalDateTime.now(), "id", "n", "e");
    }
}
