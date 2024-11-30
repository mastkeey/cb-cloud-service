package ru.mastkey.cloudservice.service;

import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    void changeCurrentWorkspace(Long telegramUserId, UUID newWorkspaceName);
}
