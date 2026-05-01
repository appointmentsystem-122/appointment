package com.appointmentscheduler.domain.events;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentEventFieldsTest {

    @Test
    void gettersAndDetailsDefault() {
        User u = new User("u", "N", "e@x.com", "h");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        AppointmentEvent ev = new AppointmentEvent(AppointmentEvent.Type.MODIFIED, a, u, null);
        assertThat(ev.getType()).isEqualTo(AppointmentEvent.Type.MODIFIED);
        assertThat(ev.getAppointment()).isSameAs(a);
        assertThat(ev.getActor()).isSameAs(u);
        assertThat(ev.getDetails()).isEmpty();
        assertThat(ev.getOccurredAt()).isNotNull();
    }
}
