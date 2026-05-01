package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UserRepository.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> dataStore = new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        if (user != null) {
            dataStore.put(user.getId(), user);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(dataStore.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return dataStore.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(dataStore.values());
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(dataStore.values());
    }
}
