package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Room;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Room entities.
 */
public interface RoomRepository {
    void save(Room room);
    Optional<Room> findById(String id);
    List<Room> findAll();
}
