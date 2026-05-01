-- Optional booking request metadata (enterprise customer portal)
ALTER TABLE appointment ADD COLUMN customer_notes VARCHAR(2000);
ALTER TABLE appointment ADD COLUMN contact_phone VARCHAR(64);
ALTER TABLE appointment ADD COLUMN reminder_channel VARCHAR(32);
ALTER TABLE appointment ADD COLUMN accessibility_needs VARCHAR(512);
ALTER TABLE appointment ADD COLUMN preferred_language VARCHAR(16);
