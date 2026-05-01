package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RoomRepository.
 */
public class InMemoryRoomRepository implements RoomRepository {

    private final Map<String, Room> store = new ConcurrentHashMap<>();

    @Override
    public void save(Room room) {
        if (room != null) store.put(room.getId(), room);
    }

    @Override
    public Optional<Room> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Room> findAll() {
        return new ArrayList<>(store.values());
    }
}
