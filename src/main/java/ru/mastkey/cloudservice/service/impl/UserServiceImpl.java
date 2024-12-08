package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.security.JwtService;
import ru.mastkey.cloudservice.service.HttpContextService;
import ru.mastkey.cloudservice.service.UserService;
import ru.mastkey.model.AuthUserRequest;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.TokenResponse;

import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final ConversionService conversionService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final HttpContextService httpContextService;
    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final S3Client s3Client;

    @Transactional
    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("Attempting to create user with username: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("User already exists with username: {}", request.getUsername());
            throw new ServiceException(ErrorType.CONFLICT, MSG_USER_ALREADY_EXIST, request.getUsername());
        }

        var user = conversionService.convert(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        log.info("User successfully created: {}", user.getId());

        s3Client.createBucketIfNotExists(user.getBucketName());
        log.info("S3 bucket initialized for user: {}", user.getId());

        var token = generateToken(user);

        var response = new CreateUserResponse();
        response.setId(user.getId());
        response.setToken(token);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse auth(AuthUserRequest authUserRequest) {
        log.info("Authenticating user: {}", authUserRequest.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authUserRequest.getUsername(), authUserRequest.getPassword())
            );
            log.info("Authentication successful for user: {}", authUserRequest.getUsername());
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user: {}", authUserRequest.getUsername());
            throw new ServiceException(ErrorType.UNAUTHORIZED, "Incorrect password");
        }

        var user = userRepository.findByUsername(authUserRequest.getUsername()).get();
        log.info("User found: {}", user.getId());

        var token = generateToken(user);

        return new TokenResponse(token);
    }

    @Override
    @Transactional
    public void addNewWorkspaceById(UUID workspaceId) {
        log.info("Adding new workspace: {} for current user", workspaceId);

        var userId = httpContextService.getUserIdFromJwtToken();
        var user = userRepository.findByUserIdWithWorkspaces(userId).orElseThrow(
                () -> {
                    log.error("User not found: {}", userId);
                    return new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, userId);
                }
        );

        var workspaceExists = user.getWorkspaces().stream()
                .anyMatch(workspace -> workspace.getId().equals(workspaceId));

        if (workspaceExists) {
            log.warn("Workspace already linked to user: {}", workspaceId);
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, workspaceId);
        }

        var workspace = workspaceRepository.findById(workspaceId).orElseThrow(
                () -> {
                    log.error("Workspace not found: {}", workspaceId);
                    return new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_FOUND, workspaceId);
                }
        );

        userWorkspaceRepository.save(
                UserWorkspace.builder()
                        .workspace(workspace)
                        .user(user)
                        .build()
        );

        log.info("Workspace: {} successfully linked to user: {}", workspaceId, userId);
    }

    private String generateToken(User user) {
        var token = jwtService.generateToken(user);
        log.info("JWT token generated for user: {}", user.getId());
        return token;
    }
}