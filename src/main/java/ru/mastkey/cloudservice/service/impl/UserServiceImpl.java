package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.service.UserService;
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.UserResponse;

import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final ConversionService conversionService;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final S3Client s3Client;

    @Transactional
    @Override
    public UserResponse createUser(CreateUserRequest request) {
        userRepository.findByTelegramUserId(request.getTelegramUserId()).ifPresent(user -> {
            throw new ServiceException(ErrorType.CONFLICT,
                    MSG_USER_ALREADY_EXIST, user.getTelegramUserId());
        });

        var user = conversionService.convert(request, User.class);

        var savedUser = userRepository.save(user);

        s3Client.createBucketIfNotExists(user.getBucketName());
        var workspace = workspaceService.createWorkspaceForNewUser(savedUser, request.getUsername());

        user.setCurrentWorkspace(workspace);
        userRepository.save(savedUser);

        return conversionService.convert(savedUser, UserResponse.class);
    }

    @Transactional
    @Override
    public void changeCurrentWorkspace(Long telegramUserId, UUID newWorkspaceId) {
        var user = userRepository.findByTelegramUserIdWithWorkspaces(telegramUserId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, telegramUserId)
        );

        var newCurrentWorkspace = user.getWorkspaces().stream()
                .filter(workspace -> workspace.getId().equals(newWorkspaceId))
                .findFirst()
                .orElseThrow(
                        () -> new ServiceException(ErrorType.NOT_FOUND, MSG_USER_DOESNT_HAVE_WORKSPACE, telegramUserId, newWorkspaceId)
                );

        user.setCurrentWorkspace(newCurrentWorkspace);
        userRepository.save(user);
    }
}
