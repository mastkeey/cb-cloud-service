--liquibase formatted sql

--changeset fetyukhin:CB-8

alter table file
    add COLUMN path VARCHAR(255);