--liquibase formatted sql

--changeset fetyukhin:CB-2

CREATE TABLE tg_user
(
    id               UUID PRIMARY KEY,
    chat_id          BIGINT    NOT NULL,
    telegram_user_id BIGINT    NOT NULL,
    created_at       TIMESTAMP NOT NULL
);

CREATE TABLE workspace
(
    id         UUID PRIMARY KEY,
    user_id    UUID       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    FOREIGN KEY (user_id) REFERENCES tg_user (id)
);

CREATE TABLE file
(
    id            UUID PRIMARY KEY,
    workspace_id  UUID         NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    relative_path VARCHAR(255) NOT NULL,
    checksum      VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    FOREIGN KEY (workspace_id) REFERENCES workspace (id)
);