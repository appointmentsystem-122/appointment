package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.authorization.Permission;
import com.appointmentscheduler.domain.authorization.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PermissionService.
 */
class PermissionServiceTest {

    private PermissionService permissionService;
    private User patient;
    private Administrator admin;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService();
        patient = new User("p1", "John Patient", "john@test.com", "hash");
        admin = new Administrator("a1", "Admin User", "admin@test.com", "hash");
    }

    @Test
    void patientHasBookAppointment() {
        assertTrue(permissionService.hasPermission(patient, Permission.BOOK_APPOINTMENT));
    }

    @Test
    void patientDoesNotHaveViewReports() {
        assertFalse(permissionService.hasPermission(patient, Permission.VIEW_REPORTS));
    }

    @Test
    void adminHasViewReports() {
        assertTrue(permissionService.hasPermission(admin, Permission.VIEW_REPORTS));
    }

    @Test
    void adminHasModifyAnyAppointment() {
        assertTrue(permissionService.hasPermission(admin, Permission.MODIFY_ANY_APPOINTMENT));
    }

    @Test
    void nullUserHasNoPermissions() {
        assertFalse(permissionService.hasPermission(null, Permission.BOOK_APPOINTMENT));
    }

    @Test
    void resolveRolePatient() {
        assertEquals(Role.PATIENT, permissionService.resolveRole(patient));
    }

    @Test
    void resolveRoleAdmin() {
        assertEquals(Role.ADMINISTRATOR, permissionService.resolveRole(admin));
    }

    @Test
    void requirePermissionThrowsWhenDenied() {
        assertThrows(SecurityException.class, () ->
                permissionService.requirePermission(patient, Permission.VIEW_REPORTS));
    }

    @Test
    void requirePermissionSucceedsWhenAllowed() {
        assertDoesNotThrow(() ->
                permissionService.requirePermission(patient, Permission.BOOK_APPOINTMENT));
    }

    @Test
    void doctorHasViewAnalytics() {
        User doctor = new DoctorUser("d1", "Dr. Smith", "doc@test.com", "hash");
        assertEquals(Role.DOCTOR, permissionService.resolveRole(doctor));
        assertTrue(permissionService.hasPermission(doctor, Permission.VIEW_ANALYTICS));
    }

    @Test
    void receptionistHasModifyAnyAppointment() {
        User receptionist = new ReceptionistUser("r1", "Jane", "rec@test.com", "hash");
        assertEquals(Role.RECEPTIONIST, permissionService.resolveRole(receptionist));
        assertTrue(permissionService.hasPermission(receptionist, Permission.MODIFY_ANY_APPOINTMENT));
    }
}
