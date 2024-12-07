package ru.mastkey.cloudservice.service;

import ru.mastkey.model.AuthUserRequest;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.TokenResponse;

import java.util.UUID;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest request);

    TokenResponse auth(AuthUserRequest authUserRequest);

    void addNewWorkspaceById(UUID workspaceId);
}
