package com.appointmentscheduler.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.ClinicRepository;
import com.appointmentscheduler.persistence.DoctorRepository;
import com.appointmentscheduler.persistence.RoomRepository;
import com.appointmentscheduler.persistence.UserRepository;

/**
 * Export/Import full system data for backup and restore (enterprise).
 * Uses simple JSON-like structure; in production would use proper JSON library.
 */
public class BackupRestoreService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomRepository;
    private final ClinicRepository clinicRepository;

    public BackupRestoreService(AppointmentRepository appointmentRepository,
                                UserRepository userRepository,
                                DoctorRepository doctorRepository,
                                RoomRepository roomRepository,
                                ClinicRepository clinicRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.roomRepository = roomRepository;
        this.clinicRepository = clinicRepository;
    }

    /**
     * Validates the output path to ensure it can be used for file operations.
     * @param outputPath the path to validate
     * @throws IOException if the path is invalid
     */
    private void validatePath(String outputPath) throws IOException {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IOException("Output path cannot be null or empty");
        }
        
        try {
            Path path = Path.of(outputPath);
            Path parent = path.getParent();
            
            if (parent != null && !Files.exists(parent)) {
                throw new IOException("Parent directory does not exist: " + parent);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Invalid file path: " + outputPath, e);
        }
    }

    /**
     * Exports a backup summary (counts and timestamp) to the given path.
     * Actual full backup would require serializing entities; here we write a manifest.
     */
    public void exportBackupManifest(String outputPath) throws IOException {
        try {
            List<Appointment> appts = appointmentRepository.findAll();
            List<User> users = userRepository.findAll();
            List<Doctor> doctors = doctorRepository.findAll();
            List<Room> rooms = roomRepository.findAll();
            List<Clinic> clinics = clinicRepository.findAll();

            StringBuilder sb = new StringBuilder();
            sb.append("# Backup Manifest - ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            sb.append("appointments=").append(appts.size()).append("\n");
            sb.append("users=").append(users.size()).append("\n");
            sb.append("doctors=").append(doctors.size()).append("\n");
            sb.append("rooms=").append(rooms.size()).append("\n");
            sb.append("clinics=").append(clinics.size()).append("\n");
            Files.writeString(Path.of(outputPath), sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException("Failed to export backup manifest", e);
        }
    }

    /**
     * Exports all appointments to CSV for backup/audit.
     */
    public void exportAppointmentsCsv(String outputPath) throws IOException {
        try {
            List<Appointment> appts = appointmentRepository.findAll().stream()
                .filter(java.util.Objects::nonNull)
                .filter(a -> !a.isDeleted())
                .collect(Collectors.toList());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(outputPath), StandardCharsets.UTF_8))) {
                w.println("id,patientId,patientName,startTime,endTime,status,doctorId,roomId,clinicId");
                for (Appointment a : appts) {
                    w.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        a.getId(),
                        a.getPatient() != null ? a.getPatient().getId() : "",
                        a.getPatient() != null ? a.getPatient().getName().replace(",", " ") : "",
                        a.getTimeSlot() != null ? a.getTimeSlot().getStartTime() : "",
                        a.getTimeSlot() != null ? a.getTimeSlot().getEndTime() : "",
                        a.getStatus(),
                        a.getDoctorId() != null ? a.getDoctorId() : "",
                        a.getRoomId() != null ? a.getRoomId() : "",
                        a.getClinicId() != null ? a.getClinicId() : ""
                    );
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to export appointments CSV", e);
        }
    }
}
