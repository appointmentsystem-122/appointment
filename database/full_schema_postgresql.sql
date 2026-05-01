-- =============================================================================
-- Appointment Booking System - PostgreSQL Schema (Full)
-- =============================================================================
-- للاستخدام في pgAdmin: أنشئ قاعدة بيانات باسم appointment ثم نفّذ هذا الملف
-- أو من Query Tool اختر قاعدة البيانات appointment ثم الصق ونفّذ
-- =============================================================================

-- إنشاء السكيمة
CREATE SCHEMA IF NOT EXISTS appointment;

-- =============================================================================
-- 1. المستخدمون (app_user) - أنواع: PATIENT, ADMINISTRATOR, DOCTOR, RECEPTIONIST
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_app_user_email ON appointment.app_user (email);
CREATE INDEX IF NOT EXISTS idx_app_user_type ON appointment.app_user (user_type);

COMMENT ON TABLE appointment.app_user IS 'المستخدمون: مرضى، أطباء، إداريون، استقبال';

-- =============================================================================
-- 2. العيادات / الفروع (clinic)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.clinic (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(512),
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE appointment.clinic IS 'الفروع أو العيادات';

-- =============================================================================
-- 3. الأطباء (doctor)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.doctor (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    specialty VARCHAR(128),
    max_appointments_per_day INT NOT NULL DEFAULT 12,
    clinic_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_doctor_clinic FOREIGN KEY (clinic_id) REFERENCES appointment.clinic(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_doctor_clinic ON appointment.doctor (clinic_id);

COMMENT ON TABLE appointment.doctor IS 'الأطباء أو مقدمي الخدمة';

-- =============================================================================
-- 4. الغرف / القاعات (room)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.room (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    clinic_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_room_clinic FOREIGN KEY (clinic_id) REFERENCES appointment.clinic(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_room_clinic ON appointment.room (clinic_id);

COMMENT ON TABLE appointment.room IS 'غرف الاستشارة أو المواعيد';

-- =============================================================================
-- 5. المواعيد (appointment)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.appointment (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    patient_id UUID NOT NULL,
    doctor_id VARCHAR(64),
    room_id VARCHAR(64),
    clinic_id VARCHAR(64),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    participant_count INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(64),
    urgent BOOLEAN NOT NULL DEFAULT false,
    appointment_type VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL',
    location VARCHAR(512),
    meeting_link VARCHAR(512),
    prior_appointment_id VARCHAR(64),
    series_id VARCHAR(64),
    occurrence_id VARCHAR(64),
    rec_frequency VARCHAR(32),
    rec_series_start TIMESTAMPTZ,
    rec_series_end TIMESTAMPTZ,
    rec_interval INT,
    max_capacity INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES appointment.app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES appointment.doctor(id) ON DELETE SET NULL,
    CONSTRAINT fk_appt_room FOREIGN KEY (room_id) REFERENCES appointment.room(id) ON DELETE SET NULL,
    CONSTRAINT fk_appt_clinic FOREIGN KEY (clinic_id) REFERENCES appointment.clinic(id) ON DELETE SET NULL,
    CONSTRAINT fk_appt_prior FOREIGN KEY (prior_appointment_id) REFERENCES appointment.appointment(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_appointment_patient ON appointment.appointment (patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_doctor ON appointment.appointment (doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointment_start ON appointment.appointment (start_time);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointment.appointment (status);
CREATE INDEX IF NOT EXISTS idx_appointment_clinic ON appointment.appointment (clinic_id);
CREATE INDEX IF NOT EXISTS idx_appointment_deleted ON appointment.appointment (deleted);

COMMENT ON TABLE appointment.appointment IS 'جدول المواعيد';

-- =============================================================================
-- 6. المهام المعلقة (pending_task) - صندوق المهام والمراجعات
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.pending_task (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    details TEXT,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pending_task_creator FOREIGN KEY (created_by_user_id) REFERENCES appointment.app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_pending_task_status ON appointment.pending_task (status);
CREATE INDEX IF NOT EXISTS idx_pending_task_creator ON appointment.pending_task (created_by_user_id);

COMMENT ON TABLE appointment.pending_task IS 'مهام المراجعة والموافقات';

-- =============================================================================
-- 7. قائمة الانتظار (waitlist_entry)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.waitlist_entry (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    patient_id UUID NOT NULL,
    requested_date DATE NOT NULL,
    preferred_start_time TIME,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_waitlist_patient FOREIGN KEY (patient_id) REFERENCES appointment.app_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_waitlist_patient ON appointment.waitlist_entry (patient_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_date ON appointment.waitlist_entry (requested_date);

COMMENT ON TABLE appointment.waitlist_entry IS 'قائمة انتظار المرضى لمواعيد أبكر';

-- =============================================================================
-- 8. سجل التدقيق (audit_entry) - للتتبع والامتثال
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.audit_entry (
    id BIGSERIAL PRIMARY KEY,
    "timestamp" TIMESTAMPTZ NOT NULL,
    user_id VARCHAR(64),
    user_name VARCHAR(255),
    action VARCHAR(128) NOT NULL,
    details TEXT,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    old_value TEXT,
    new_value TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON appointment.audit_entry ("timestamp");
CREATE INDEX IF NOT EXISTS idx_audit_user ON appointment.audit_entry (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON appointment.audit_entry (entity_type, entity_id);

COMMENT ON TABLE appointment.audit_entry IS 'سجل تدقيق (قراءة فقط)';

-- =============================================================================
-- 9. إعدادات النظام (system_settings) - مفاتيح وقيم
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment.system_settings (
    key VARCHAR(255) NOT NULL PRIMARY KEY,
    value TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE appointment.system_settings IS 'إعدادات التطبيق (مفتاح-قيمة)';

-- =============================================================================
-- إنهاء
-- =============================================================================
-- تم إنشاء السكيمة appointment وجميع الجداول.
-- تأكد من إنشاء المستخدم appointments_user ومنحه الصلاحيات على السكيمة appointment.
-- مثال منح صلاحيات (نفّذها بصلاحية مالك القاعدة):
--   GRANT USAGE ON SCHEMA appointment TO appointments_user;
--   GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA appointment TO appointments_user;
--   GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA appointment TO appointments_user;
--   ALTER DEFAULT PRIVILEGES IN SCHEMA appointment GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO appointments_user;
