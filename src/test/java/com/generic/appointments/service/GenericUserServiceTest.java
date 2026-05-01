package com.generic.appointments.service;

import com.generic.appointments.model.Customer;
import com.generic.appointments.repository.impl.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenericUserServiceTest {

    @Test
    void registerFindAllFindByEmail() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        UserService svc = new UserService(repo);
        Customer c = new Customer(null, "U", "u@x.com");
        svc.registerUser(c);
        assertThat(svc.findAll()).hasSize(1);
        assertThat(svc.findByEmail("U@X.COM")).contains(c);
    }
}
