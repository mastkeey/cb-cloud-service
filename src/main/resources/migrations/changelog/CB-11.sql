--liquibase formatted sql

--changeset fetyukhin:CB-11

ALTER TABLE workspace ALTER COLUMN name TYPE TEXT;
ALTER TABLE file ALTER COLUMN file_extension TYPE TEXT;
ALTER TABLE file ALTER COLUMN file_name TYPE TEXT;
ALTER TABLE file ALTER COLUMN path TYPE TEXT;