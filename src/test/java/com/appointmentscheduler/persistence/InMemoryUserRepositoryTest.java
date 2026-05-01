package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserRepositoryTest {

    @Test
    void saveFindAndEnumerateUsers() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        User u = new User("u1", "Name", "user@x.com", "h");
        repo.save(u);

        assertThat(repo.findById("u1")).contains(u);
        assertThat(repo.findByEmail("USER@x.com")).contains(u);
        assertThat(repo.getAllUsers()).containsExactly(u);
        assertThat(repo.findAll()).containsExactly(u);
    }

    @Test
    void saveNullAndUnknownEmail_areHandled() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        repo.save(null);
        assertThat(repo.findAll()).isEmpty();
        assertThat(repo.findByEmail("missing@x.com")).isEmpty();
    }
}
