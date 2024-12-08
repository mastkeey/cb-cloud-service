--liquibase formatted sql

--changeset fetyukhin:CB-10

ALTER TABLE workspace
    ADD CONSTRAINT fk_workspace_owner
        FOREIGN KEY (owner_id) REFERENCES tg_user (id) ON DELETE CASCADE;