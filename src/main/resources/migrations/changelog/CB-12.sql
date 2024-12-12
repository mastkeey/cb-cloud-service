--liquibase formatted sql

--changeset fetyukhin:CB-11

ALTER TABLE users ALTER COLUMN bucket_name TYPE TEXT;
