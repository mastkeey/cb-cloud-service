--liquibase formatted sql

--changeset fetyukhin:CB-11

ALTER TABLE workspace ALTER COLUMN name TYPE TEXT;
ALTER TABLE file ALTER COLUMN file_extension TYPE TEXT;
ALTER TABLE file ALTER COLUMN file_name TYPE TEXT;
ALTER TABLE file ALTER COLUMN path TYPE TEXT;
ALTER TABLE tg_user ALTER COLUMN username TYPE TEXT;
ALTER TABLE tg_user ALTER COLUMN password TYPE TEXT;
ALTER TABLE tg_user RENAME TO users;
ALTER TABLE file RENAME TO files;
ALTER TABLE workspace RENAME TO workspaces;