package ru.mastkey.cloudservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.mastkey.api.UserControllerApi;
import ru.mastkey.cloudservice.service.UserService;
import ru.mastkey.model.AuthUserRequest;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.TokenResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {
    private final UserService userService;

    @Override
    public ResponseEntity<Void> addNewWorkspace(UUID uuid, UUID uuid1) {
        return null;
    }

    @Override
    public ResponseEntity<TokenResponse> authUser(AuthUserRequest authUserRequest) {
        return ResponseEntity.ok(userService.auth(authUserRequest));
    }

    @Override
    public ResponseEntity<CreateUserResponse> createUser(CreateUserRequest createUserRequest) {
        return ResponseEntity.ok(userService.createUser(createUserRequest));
    }
}
