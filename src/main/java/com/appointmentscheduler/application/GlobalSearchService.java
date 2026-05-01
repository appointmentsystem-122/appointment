package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides global search across appointments and users.
 */
public class GlobalSearchService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public GlobalSearchService(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    public static final class SearchResult {
        public final String type; // "appointment", "user"
        public final String id;
        public final String title;
        public final String subtitle;

        public SearchResult(String type, String id, String title, String subtitle) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    /**
     * Searches appointments (by patient name, email, slot, id) and users (name, email).
     * Returns up to maxResults combined results.
     */
    public List<SearchResult> search(String term, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        if (term == null || term.isBlank()) return results;
        String lower = term.trim().toLowerCase();

        List<Appointment> appts = appointmentRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isDeleted())
                .filter(a -> matchesAppointment(a, lower))
                .limit(maxResults / 2)
                .collect(Collectors.toList());
        for (Appointment a : appts) {
            String patientLabel = a.getPatient() != null && a.getPatient().getName() != null
                    ? a.getPatient().getName()
                    : "";
            String slotStr = a.getTimeSlot() != null ? a.getTimeSlot().toString() : "";
            results.add(new SearchResult("appointment", a.getId(), slotStr,
                    patientLabel + " – " + (a.getStatus() != null ? a.getStatus() : "")));
        }

        List<User> users = userRepository.findAll().stream()
                .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(lower))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(lower)))
                .limit(maxResults / 2)
                .collect(Collectors.toList());
        for (User u : users) {
            results.add(new SearchResult("user", u.getId(), u.getName(), u.getEmail()));
        }

        return results.stream().limit(maxResults).collect(Collectors.toList());
    }

    private boolean matchesAppointment(Appointment a, String lower) {
        if (a.getPatient() != null && a.getPatient().getName() != null && a.getPatient().getName().toLowerCase().contains(lower)) return true;
        if (a.getPatient() != null && a.getPatient().getEmail() != null && a.getPatient().getEmail().toLowerCase().contains(lower)) return true;
        if (a.getId() != null && a.getId().toLowerCase().contains(lower)) return true;
        if (a.getTimeSlot() != null && a.getTimeSlot().toString().toLowerCase().contains(lower)) return true;
        return false;
    }
}
