package ru.mastkey.cloudservice.service;

import java.util.UUID;

public interface HttpContextService {
    UUID getUserIdFromJwtToken();
}
