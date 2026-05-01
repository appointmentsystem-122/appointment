-- =============================================================================
-- Appointment Booking System - Database Schema (Single File)
-- =============================================================================
-- Version: 1.0
-- Scope: Full project persistence in one H2 file database.
-- Tables: users, clinics, doctors, rooms, appointments, optional legacy tasks/waitlist, audit,
--        system_settings.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. USERS (Single-table inheritance: PATIENT, ADMINISTRATOR, DOCTOR, RECEPTIONIST)
-- -----------------------------------------------------------------------------
CREATE TABLE app_user (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(32) NOT NULL DEFAULT 'PATIENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_user_type ON app_user(user_type);

-- -----------------------------------------------------------------------------
-- 2. CLINICS (Branches / multi-tenant)
-- -----------------------------------------------------------------------------
CREATE TABLE clinic (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(512),
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 3. DOCTORS (Resources; optional link to clinic)
-- -----------------------------------------------------------------------------
CREATE TABLE doctor (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    specialty VARCHAR(128),
    max_appointments_per_day INT NOT NULL DEFAULT 12,
    clinic_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doctor_clinic FOREIGN KEY (clinic_id) REFERENCES clinic(id)
);
CREATE INDEX idx_doctor_clinic ON doctor(clinic_id);

-- -----------------------------------------------------------------------------
-- 4. ROOMS (Resources; optional link to clinic)
-- -----------------------------------------------------------------------------
CREATE TABLE room (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    clinic_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_clinic FOREIGN KEY (clinic_id) REFERENCES clinic(id)
);
CREATE INDEX idx_room_clinic ON room(clinic_id);

-- -----------------------------------------------------------------------------
-- 5. APPOINTMENTS (Single-table inheritance + type-specific columns)
-- -----------------------------------------------------------------------------
CREATE TABLE appointment (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64),
    room_id VARCHAR(64),
    clinic_id VARCHAR(64),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    participant_count INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(64),
    urgent BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_type VARCHAR(32) NOT NULL,
    location VARCHAR(512),
    meeting_link VARCHAR(512),
    prior_appointment_id VARCHAR(64),
    series_id VARCHAR(64),
    occurrence_id VARCHAR(64),
    rec_frequency VARCHAR(32),
    rec_series_start TIMESTAMP,
    rec_series_end TIMESTAMP,
    rec_interval INT,
    max_capacity INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES app_user(id),
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id),
    CONSTRAINT fk_appt_room FOREIGN KEY (room_id) REFERENCES room(id),
    CONSTRAINT fk_appt_clinic FOREIGN KEY (clinic_id) REFERENCES clinic(id),
    CONSTRAINT fk_appt_prior FOREIGN KEY (prior_appointment_id) REFERENCES appointment(id)
);
CREATE INDEX idx_appt_patient ON appointment(patient_id);
CREATE INDEX idx_appt_doctor ON appointment(doctor_id);
CREATE INDEX idx_appt_start ON appointment(start_time);
CREATE INDEX idx_appt_status ON appointment(status);
CREATE INDEX idx_appt_clinic ON appointment(clinic_id);
CREATE INDEX idx_appt_deleted ON appointment(deleted);

-- -----------------------------------------------------------------------------
-- 6. PENDING TASKS (Task Inbox: reviews, approvals)
-- -----------------------------------------------------------------------------
CREATE TABLE pending_task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    details CLOB,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    created_by_user_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_pending_task_status ON pending_task(status);
CREATE INDEX idx_pending_task_creator ON pending_task(created_by_user_id);

-- -----------------------------------------------------------------------------
-- 7. WAITLIST (Patients waiting for earlier slots)
-- -----------------------------------------------------------------------------
CREATE TABLE waitlist_entry (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    requested_date DATE NOT NULL,
    preferred_start_time TIME,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_waitlist_patient FOREIGN KEY (patient_id) REFERENCES app_user(id)
);
CREATE INDEX idx_waitlist_patient ON waitlist_entry(patient_id);
CREATE INDEX idx_waitlist_date ON waitlist_entry(requested_date);

-- -----------------------------------------------------------------------------
-- 8. AUDIT TRAIL (Append-only compliance log)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(64),
    user_name VARCHAR(255),
    action VARCHAR(128) NOT NULL,
    details CLOB,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    old_value CLOB,
    new_value CLOB
);
CREATE INDEX idx_audit_timestamp ON audit_entry(timestamp);
CREATE INDEX idx_audit_user ON audit_entry(user_id);
CREATE INDEX idx_audit_entity ON audit_entry(entity_type, entity_id);

-- -----------------------------------------------------------------------------
-- 9. SYSTEM SETTINGS (Enterprise key-value overrides)
-- -----------------------------------------------------------------------------
CREATE TABLE system_settings (
    "key" VARCHAR(255) NOT NULL PRIMARY KEY,
    "value" CLOB,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
