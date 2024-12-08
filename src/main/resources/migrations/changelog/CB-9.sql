--liquibase formatted sql

--changeset fetyukhin:CB-9

ALTER TABLE tg_user DROP COLUMN chat_id;
ALTER TABLE tg_user DROP COLUMN telegram_user_id;

ALTER TABLE tg_user ADD COLUMN username varchar(255);
ALTER TABLE tg_user ADD COLUMN password varchar(255);

ALTER TABLE tg_user DROP CONSTRAINT fk_tg_user_current_workspace;
ALTER TABLE tg_user DROP COLUMN current_workspace_id;

CREATE TABLE user_workspace
(
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,
    workspace_id UUID NOT NULL,
    FOREIGN KEY (user_id) REFERENCES tg_user (id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id) REFERENCES workspace (id) ON DELETE CASCADE
);

ALTER TABLE workspace DROP COLUMN user_id;
ALTER TABLE workspace
    ADD COLUMN owner_id UUID;
