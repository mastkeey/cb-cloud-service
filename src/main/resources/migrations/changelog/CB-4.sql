--liquibase formatted sql

--changeset fetyukhin:CB-4

ALTER table file drop column checksum;
ALTER table file drop column relative_path;
ALTER table file
    add column file_extension VARCHAR(255);
ALTER table tg_user
    add column bucket_name VARCHAR(255);
ALTER TABLE tg_user
    ADD COLUMN current_workspace_id UUID;

ALTER TABLE tg_user
    ADD CONSTRAINT fk_tg_user_current_workspace
        FOREIGN KEY (current_workspace_id) REFERENCES workspace (id) ON DELETE SET NULL;