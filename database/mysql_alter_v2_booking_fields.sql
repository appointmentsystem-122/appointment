-- Run in phpMyAdmin if appointment table exists but lacks booking-extra columns (matches Flyway V2).
-- Safe to run once; duplicate column errors can be ignored.

USE `appointment`;

ALTER TABLE `appointment` ADD COLUMN `customer_notes` VARCHAR(2000) NULL;
ALTER TABLE `appointment` ADD COLUMN `contact_phone` VARCHAR(64) NULL;
ALTER TABLE `appointment` ADD COLUMN `reminder_channel` VARCHAR(32) NULL;
ALTER TABLE `appointment` ADD COLUMN `accessibility_needs` VARCHAR(512) NULL;
ALTER TABLE `appointment` ADD COLUMN `preferred_language` VARCHAR(16) NULL;
