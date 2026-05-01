package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entities.
 */
public interface UserRepository {
    
    /**
     * Saves a user to the repository.
     * @param user the user to save
     */
    void save(User user);

    /**
     * Finds a user by ID.
     * @param id the user ID
     * @return an Optional containing the user if found, or empty if not
     */
    Optional<User> findById(String id);

    /**
     * Finds a user by their email address.
     * @param email the email
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Retrieves all registered users.
     * @return list of all users
     */
    List<User> getAllUsers();

    /**
     * Retrieves all users.
     * @return a list of all users
     */
    List<User> findAll();
}
