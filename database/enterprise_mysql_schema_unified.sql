-- =============================================================================
-- Appointment Booking System — Enterprise MySQL / MariaDB Schema (Unified)
-- =============================================================================
-- Schema version:     2.0.0
-- Aligns with:        Flyway V1 (enterprise_schema) + V2 (booking_request_fields)
-- Target engines:     InnoDB, utf8mb4
-- Compatibility:      MySQL 5.7+ / 8.0+  ·  MariaDB 10.2+
--
-- Contents:
--   • Fresh install:   database, all tables, FKs, indexes, views
--   • Booking extras:  customer_notes, contact_phone, reminder_channel,
--                      accessibility_needs, preferred_language (V2)
--   • Optional §7:     idempotent upgrades for existing DBs (pre-V2)
--
-- Deployment (greenfield):
--   Import in phpMyAdmin / mysql CLI:  mysql -u user -p < enterprise_mysql_schema_unified.sql
--
-- Deployment (brownfield / existing DB missing V2 columns):
--   Run section §7 only, or rely on Java DatabaseConfig.ensureMySqlAppointmentBookingColumns()
--
-- / نسخة موحّدة لبيئات الشركات — جميع التعديلات الحالية مدمجة في ملف واحد
-- =============================================================================

SET NAMES utf8mb4;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

/* -----------------------------------------------------------------------------
   §1 — Database
----------------------------------------------------------------------------- */
CREATE DATABASE IF NOT EXISTS `appointment`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `appointment`;

SET FOREIGN_KEY_CHECKS = 0;

/* -----------------------------------------------------------------------------
   §2 — Core identity & multi-tenant (users, clinics, doctors, rooms)
----------------------------------------------------------------------------- */

CREATE TABLE IF NOT EXISTS `app_user` (
  `id` CHAR(36) NOT NULL COMMENT 'UUID — PATIENT, ADMINISTRATOR, DOCTOR, RECEPTIONIST',
  `name` VARCHAR(200) NOT NULL,
  `email` VARCHAR(320) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `user_type` VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_app_user_email` (`email`),
  KEY `idx_app_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Users — single-table inheritance for roles';

CREATE TABLE IF NOT EXISTS `clinic` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `address` VARCHAR(512) DEFAULT NULL,
  `time_zone` VARCHAR(64) NOT NULL DEFAULT 'UTC',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Branches / tenants';

CREATE TABLE IF NOT EXISTS `doctor` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) DEFAULT NULL,
  `specialty` VARCHAR(128) DEFAULT NULL,
  `max_appointments_per_day` INT NOT NULL DEFAULT 12,
  `clinic_id` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_doctor_clinic` (`clinic_id`),
  CONSTRAINT `fk_doctor_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Providers — optional clinic scope';

CREATE TABLE IF NOT EXISTS `room` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `clinic_id` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_room_clinic` (`clinic_id`),
  CONSTRAINT `fk_room_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Rooms / resources';

/* -----------------------------------------------------------------------------
   §3 — Appointments (V1 single-table inheritance + V2 booking request metadata)
----------------------------------------------------------------------------- */

CREATE TABLE IF NOT EXISTS `appointment` (
  `id` VARCHAR(64) NOT NULL,
  `patient_id` CHAR(36) NOT NULL,
  `doctor_id` VARCHAR(64) DEFAULT NULL,
  `room_id` VARCHAR(64) DEFAULT NULL,
  `clinic_id` VARCHAR(64) DEFAULT NULL,
  `start_time` DATETIME(6) NOT NULL,
  `end_time` DATETIME(6) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CONFIRMED, CANCELLED, EXPIRED, COMPLETED',
  `participant_count` INT NOT NULL DEFAULT 1,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `deleted_at` DATETIME(6) DEFAULT NULL,
  `deleted_by` VARCHAR(64) DEFAULT NULL,
  `urgent` TINYINT(1) NOT NULL DEFAULT 0,
  `appointment_type` VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL' COMMENT 'IN_PERSON, VIRTUAL, FOLLOW_UP, RECURRING, GROUP, INDIVIDUAL, ASSESSMENT, URGENT',
  `location` VARCHAR(512) DEFAULT NULL,
  `meeting_link` VARCHAR(512) DEFAULT NULL,
  `prior_appointment_id` VARCHAR(64) DEFAULT NULL,
  `series_id` VARCHAR(64) DEFAULT NULL,
  `occurrence_id` VARCHAR(64) DEFAULT NULL,
  `rec_frequency` VARCHAR(32) DEFAULT NULL,
  `rec_series_start` DATETIME(6) DEFAULT NULL,
  `rec_series_end` DATETIME(6) DEFAULT NULL,
  `rec_interval` INT DEFAULT NULL,
  `max_capacity` INT DEFAULT NULL,
  /* —— V2: customer portal / booking extras (Flyway V2__booking_request_fields) —— */
  `customer_notes` VARCHAR(2000) DEFAULT NULL COMMENT 'Free-text notes from booking flow',
  `contact_phone` VARCHAR(64) DEFAULT NULL COMMENT 'Contact for reminders',
  `reminder_channel` VARCHAR(32) DEFAULT NULL COMMENT 'e.g. SMS, EMAIL, APP',
  `accessibility_needs` VARCHAR(512) DEFAULT NULL,
  `preferred_language` VARCHAR(16) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_appointment_patient` (`patient_id`),
  KEY `idx_appointment_doctor` (`doctor_id`),
  KEY `idx_appointment_start` (`start_time`),
  KEY `idx_appointment_status` (`status`),
  KEY `idx_appointment_clinic` (`clinic_id`),
  KEY `idx_appointment_deleted` (`deleted`),
  /* Composite indexes — common enterprise query patterns */
  KEY `idx_appointment_clinic_start` (`clinic_id`, `start_time`),
  KEY `idx_appointment_patient_status` (`patient_id`, `status`),
  CONSTRAINT `fk_appt_patient` FOREIGN KEY (`patient_id`) REFERENCES `app_user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_prior` FOREIGN KEY (`prior_appointment_id`) REFERENCES `appointment` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Appointments — STI + recurrence + V2 booking metadata';

/* -----------------------------------------------------------------------------
   §4 — Workflow & compliance
----------------------------------------------------------------------------- */

CREATE TABLE IF NOT EXISTS `pending_task` (
  `id` VARCHAR(64) NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `title` VARCHAR(512) NOT NULL,
  `details` TEXT DEFAULT NULL,
  `entity_type` VARCHAR(64) DEFAULT NULL,
  `entity_id` VARCHAR(64) DEFAULT NULL,
  `created_by_user_id` CHAR(36) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_pending_task_status` (`status`),
  KEY `idx_pending_task_creator` (`created_by_user_id`),
  CONSTRAINT `fk_pending_task_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `app_user` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Task inbox — reviews / approvals';

CREATE TABLE IF NOT EXISTS `waitlist_entry` (
  `id` VARCHAR(64) NOT NULL,
  `patient_id` CHAR(36) NOT NULL,
  `requested_date` DATE NOT NULL,
  `preferred_start_time` TIME DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_waitlist_patient` (`patient_id`),
  KEY `idx_waitlist_date` (`requested_date`),
  CONSTRAINT `fk_waitlist_patient` FOREIGN KEY (`patient_id`) REFERENCES `app_user` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Waitlist';

CREATE TABLE IF NOT EXISTS `audit_entry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `timestamp` DATETIME(6) NOT NULL,
  `user_id` VARCHAR(64) DEFAULT NULL,
  `user_name` VARCHAR(255) DEFAULT NULL,
  `action` VARCHAR(128) NOT NULL,
  `details` TEXT DEFAULT NULL,
  `entity_type` VARCHAR(64) DEFAULT NULL,
  `entity_id` VARCHAR(64) DEFAULT NULL,
  `old_value` TEXT DEFAULT NULL,
  `new_value` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_timestamp` (`timestamp`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Append-only audit trail';

CREATE TABLE IF NOT EXISTS `system_settings` (
  `key` VARCHAR(255) NOT NULL,
  `value` TEXT DEFAULT NULL,
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Key-value configuration overrides';

SET FOREIGN_KEY_CHECKS = 1;

/* -----------------------------------------------------------------------------
   §5 — Views (read-only reporting / ops)
----------------------------------------------------------------------------- */

DROP VIEW IF EXISTS `v_appointments_simple`;
CREATE VIEW `v_appointments_simple` AS
SELECT
  `id`,
  `patient_id`,
  `doctor_id`,
  `clinic_id`,
  `start_time`,
  `end_time`,
  `status`,
  `appointment_type`,
  `created_at`
FROM `appointment`
WHERE `deleted` = 0;

DROP VIEW IF EXISTS `v_appointments_booking_extras`;
CREATE VIEW `v_appointments_booking_extras` AS
SELECT
  `id`,
  `patient_id`,
  `clinic_id`,
  `start_time`,
  `end_time`,
  `status`,
  `customer_notes`,
  `contact_phone`,
  `reminder_channel`,
  `accessibility_needs`,
  `preferred_language`,
  `updated_at`
FROM `appointment`
WHERE `deleted` = 0;

/* -----------------------------------------------------------------------------
   §6 — Optional: grants (uncomment & replace placeholders in production)
----------------------------------------------------------------------------- */
-- CREATE USER IF NOT EXISTS 'appt_app'@'%' IDENTIFIED BY '***strong_password***';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `appointment`.* TO 'appt_app'@'%';
-- FLUSH PRIVILEGES;

/* -----------------------------------------------------------------------------
   §7 — Upgrade path — existing installations created before V2
         Run once if `appointment` exists but V2 columns are missing.
         MySQL 8.0.12+ / MariaDB 10.5.2+: IF NOT EXISTS supported.
         Older servers: use mysql_alter_v2_booking_fields.sql or run ALTERs manually.
----------------------------------------------------------------------------- */

-- Uncomment the block below only for upgrades (not needed on fresh §3 install):

-- SET FOREIGN_KEY_CHECKS = 0;
-- ALTER TABLE `appointment` ADD COLUMN IF NOT EXISTS `customer_notes` VARCHAR(2000) NULL COMMENT 'Free-text notes from booking flow';
-- ALTER TABLE `appointment` ADD COLUMN IF NOT EXISTS `contact_phone` VARCHAR(64) NULL;
-- ALTER TABLE `appointment` ADD COLUMN IF NOT EXISTS `reminder_channel` VARCHAR(32) NULL;
-- ALTER TABLE `appointment` ADD COLUMN IF NOT EXISTS `accessibility_needs` VARCHAR(512) NULL;
-- ALTER TABLE `appointment` ADD COLUMN IF NOT EXISTS `preferred_language` VARCHAR(16) NULL;
-- SET FOREIGN_KEY_CHECKS = 1;

/* -----------------------------------------------------------------------------
   §8 — Post-deploy: composite indexes on legacy DBs (skip errors if exist)
         Safe if you already created appointment from an older script without §3 composites.
----------------------------------------------------------------------------- */

-- CREATE INDEX `idx_appointment_clinic_start` ON `appointment` (`clinic_id`, `start_time`);
-- CREATE INDEX `idx_appointment_patient_status` ON `appointment` (`patient_id`, `status`);

-- =============================================================================
-- End of schema 2.0.0 — Appointment Booking System
-- =============================================================================
