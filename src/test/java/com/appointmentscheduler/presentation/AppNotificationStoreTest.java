package com.appointmentscheduler.presentation;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class AppNotificationStoreTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void getRecent_whenFewerThanMax_returnsFullCopy() {
        AppNotificationStore s = new AppNotificationStore();
        s.add("a", "m");
        s.add("b", "m", true);
        List<AppNotificationStore.Entry> r = s.getRecent(10);
        assertThat(r).hasSize(2);
        assertThat(r.get(1).isError()).isTrue();
    }

    @Test
    void getRecent_whenMoreThanMax_returnsTail() {
        AppNotificationStore s = new AppNotificationStore();
        IntStream.range(0, 5).forEach(i -> s.add("t" + i, "m"));
        assertThat(s.getRecent(2)).hasSize(2);
        assertThat(s.getRecent(2).get(0).getTitle()).isEqualTo("t3");
    }

    @Test
    void add_trimsToMaxItems() {
        AppNotificationStore s = new AppNotificationStore();
        IntStream.range(0, 55).forEach(i -> s.add("x", "y"));
        assertThat(s.getRecent(200)).hasSize(50);
    }

    @Test
    void entry_nullTitleMessage_coalesced() {
        AppNotificationStore.Entry e = new AppNotificationStore.Entry(null, null, null, false);
        assertThat(e.getTitle()).isEmpty();
        assertThat(e.getMessage()).isEmpty();
        assertThat(e.getTimeFormatted()).matches("\\d{2}:\\d{2}");
    }

    @Test
    void getObservableRecent_wraps() {
        AppNotificationStore s = new AppNotificationStore();
        s.add("a", "b");
        ObservableList<AppNotificationStore.Entry> obs = s.getObservableRecent(5);
        assertThat(obs).hasSize(1);
    }

    @Test
    void unreadCount_followsStoreSize_boundedByMaxUnread() {
        AppNotificationStore s = new AppNotificationStore();
        IntStream.range(0, 120).forEach(i -> s.add("t", "m"));
        // Storage is capped at 50 entries before unread count can reach 99.
        assertThat(s.getRecent(200)).hasSize(50);
        assertThat(s.getUnreadCount()).isEqualTo(50);
    }

    @Test
    void clear_empties() {
        AppNotificationStore s = new AppNotificationStore();
        s.add("a", "b");
        s.clear();
        assertThat(s.getRecent(5)).isEmpty();
    }
}
