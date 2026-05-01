package com.appointmentscheduler.domain.events;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentEventPublisherTest {

    @Test
    void publishNotifiesListeners() {
        AppointmentEventPublisher pub = new AppointmentEventPublisher();
        AtomicReference<AppointmentEvent> seen = new AtomicReference<>();
        AppointmentEventListener l = seen::set;
        pub.addListener(l);
        pub.addListener(l);
        User u = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        AppointmentEvent ev = new AppointmentEvent(AppointmentEvent.Type.CREATED, a, u, "d");
        pub.publish(ev);
        assertThat(seen.get()).isSameAs(ev);
        pub.removeListener(l);
        pub.publish(ev);
    }

    @Test
    void addListener_null_isIgnored() {
        AppointmentEventPublisher pub = new AppointmentEventPublisher();
        pub.addListener(null);
        AtomicInteger calls = new AtomicInteger();
        pub.addListener(e -> calls.incrementAndGet());
        User u = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        AppointmentEvent ev = new AppointmentEvent(AppointmentEvent.Type.CREATED, a, u, "d");
        pub.publish(ev);
        assertThat(calls.get()).isEqualTo(1);
    }
}
