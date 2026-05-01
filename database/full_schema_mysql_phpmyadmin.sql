-- =============================================================================
-- Appointment Booking System - MySQL / MariaDB - قاعدة بيانات كاملة
-- =============================================================================
-- للاستخدام في phpMyAdmin: استورد هذا الملف (Import) أو الصق ونفّذ
-- متوافق مع MySQL 5.7+ و MariaDB 10.2+
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- إنشاء قاعدة البيانات
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `appointment`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `appointment`;

-- =============================================================================
-- 1. المستخدمون (app_user)
-- أنواع: PATIENT, ADMINISTRATOR, DOCTOR, RECEPTIONIST
-- =============================================================================
CREATE TABLE IF NOT EXISTS `app_user` (
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `email` VARCHAR(320) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `user_type` VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_app_user_email` (`email`),
  KEY `idx_app_user_email` (`email`),
  KEY `idx_app_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='المستخدمون: مرضى، أطباء، إداريون، استقبال';

-- =============================================================================
-- 2. العيادات / الفروع (clinic)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `clinic` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `address` VARCHAR(512) DEFAULT NULL,
  `time_zone` VARCHAR(64) NOT NULL DEFAULT 'UTC',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='الفروع أو العيادات';

-- =============================================================================
-- 3. الأطباء (doctor)
-- =============================================================================
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
  CONSTRAINT `fk_doctor_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='الأطباء أو مقدمي الخدمة';

-- =============================================================================
-- 4. الغرف / القاعات (room)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `room` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `clinic_id` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_room_clinic` (`clinic_id`),
  CONSTRAINT `fk_room_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='غرف الاستشارة أو المواعيد';

-- =============================================================================
-- 5. المواعيد (appointment)
-- الأعمدة الكثيرة لدعم: نوع الموعد، الحذف المنطقي، التكرار، الرابط الافتراضي، إلخ.
-- للعرض المبسّط فقط استخدم العرض v_appointments_simple.
-- =============================================================================
CREATE TABLE IF NOT EXISTS `appointment` (
  `id` VARCHAR(64) NOT NULL,
  `patient_id` CHAR(36) NOT NULL,
  `doctor_id` VARCHAR(64) DEFAULT NULL,
  `room_id` VARCHAR(64) DEFAULT NULL,
  `clinic_id` VARCHAR(64) DEFAULT NULL,
  `start_time` DATETIME(6) NOT NULL,
  `end_time` DATETIME(6) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `participant_count` INT NOT NULL DEFAULT 1,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `deleted_at` DATETIME(6) DEFAULT NULL,
  `deleted_by` VARCHAR(64) DEFAULT NULL,
  `urgent` TINYINT(1) NOT NULL DEFAULT 0,
  `appointment_type` VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL',
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
  `customer_notes` VARCHAR(2000) DEFAULT NULL,
  `contact_phone` VARCHAR(64) DEFAULT NULL,
  `reminder_channel` VARCHAR(32) DEFAULT NULL,
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
  CONSTRAINT `fk_appt_patient` FOREIGN KEY (`patient_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_clinic` FOREIGN KEY (`clinic_id`) REFERENCES `clinic` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_appt_prior` FOREIGN KEY (`prior_appointment_id`) REFERENCES `appointment` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='جدول المواعيد';

-- =============================================================================
-- 6. المهام المعلقة (pending_task)
-- =============================================================================
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
  CONSTRAINT `fk_pending_task_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `app_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='مهام المراجعة والموافقات';

-- =============================================================================
-- 7. قائمة الانتظار (waitlist_entry)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `waitlist_entry` (
  `id` VARCHAR(64) NOT NULL,
  `patient_id` CHAR(36) NOT NULL,
  `requested_date` DATE NOT NULL,
  `preferred_start_time` TIME DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_waitlist_patient` (`patient_id`),
  KEY `idx_waitlist_date` (`requested_date`),
  CONSTRAINT `fk_waitlist_patient` FOREIGN KEY (`patient_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='قائمة انتظار المرضى لمواعيد أبكر';

-- =============================================================================
-- 8. سجل التدقيق (audit_entry)
-- =============================================================================
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
COMMENT='سجل تدقيق (قراءة فقط)';

-- =============================================================================
-- 9. إعدادات النظام (system_settings)
-- =============================================================================
CREATE TABLE IF NOT EXISTS `system_settings` (
  `key` VARCHAR(255) NOT NULL,
  `value` TEXT DEFAULT NULL,
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='إعدادات التطبيق (مفتاح-قيمة)';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- عرض مبسّط للمواعيد (للعرض السريع في phpMyAdmin بدون كل الأعمدة)
-- =============================================================================
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
WHERE `deleted` = 0
ORDER BY `start_time` DESC;

-- =============================================================================
-- إنهاء
-- =============================================================================
-- تم إنشاء قاعدة البيانات appointment وجميع الجداول.
-- للعرض المبسّط: استخدم الجدول v_appointments_simple (عرض) بدل appointment.
-- في phpMyAdmin: استورد هذا الملف من تبويب Import أو الصق في SQL ونفّذ.
-- لربط تطبيق الجافا: application.properties → database.url مع MySQL
-- =============================================================================
