package com.generic.appointments.service;

import com.generic.appointments.model.User;
import com.generic.appointments.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * User-related use cases (registration, lookup).
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}

