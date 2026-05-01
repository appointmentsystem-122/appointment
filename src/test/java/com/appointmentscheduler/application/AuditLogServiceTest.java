package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogServiceTest {

    @Test
    void defaultRepository_inMemory_appendAndQuery() {
        AuditLogService svc = new AuditLogService();
        svc.log("ACTION", "details");
        User u = new User("u1", "N", "e@x.com", "x");
        svc.log(u, "A2", "d2");
        svc.log(u, "A3", "d3", "Ent", "e1", "old", "new");
        svc.log("id", "name", "A4", "d4");
        assertThat(svc.getRecentEntries(5)).isNotEmpty();
        assertThat(svc.getAllEntries()).isNotEmpty();
        assertThat(svc.getEntriesByEntityType("Ent")).isNotEmpty();
        assertThat(svc.getEntriesByUser("u1")).isNotEmpty();
    }
}
