-- =============================================================================
-- منح صلاحيات المستخدم appointments_user على السكيمة appointment
-- =============================================================================
-- نفّذ هذا الملف بعد full_schema_postgresql.sql
-- يجب أن تكون متصلاً بقاعدة appointment (أو postgres) وبصلاحية مالك
-- =============================================================================

-- الاتصال بقاعدة appointment (من pgAdmin اختر قاعدة appointment ثم Query Tool)

GRANT USAGE ON SCHEMA appointment TO appointments_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA appointment TO appointments_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA appointment TO appointments_user;

-- للجداول التي تُنشأ لاحقاً (اختياري):
ALTER DEFAULT PRIVILEGES IN SCHEMA appointment
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO appointments_user;
