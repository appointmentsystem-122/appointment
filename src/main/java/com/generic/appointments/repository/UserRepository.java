package com.generic.appointments.repository;

import com.generic.appointments.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing users (administrators and customers).
 */
public interface UserRepository {

    void save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    List<User> findAll();
}

