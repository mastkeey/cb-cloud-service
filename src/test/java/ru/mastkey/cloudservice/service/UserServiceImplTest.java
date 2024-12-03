package ru.mastkey.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.service.impl.UserServiceImpl;
import ru.mastkey.cloudservice.service.impl.WorkspaceServiceImpl;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private ConversionService conversionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWorkspaceRepository userWorkspaceRepository;

    @Mock
    private WorkspaceServiceImpl workspaceService;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest createUserRequest;
    private User user;
    private CreateUserResponse userResponse;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");

        user = new User();
        user.setId(UUID.randomUUID());

        userResponse = new CreateUserResponse();
        userResponse.setId(user.getId());

    }

    @Test
    void createUser_ShouldCreateUserSuccessfully() {
        when(conversionService.convert(createUserRequest, User.class))
                .thenReturn(user);
        when(userRepository.save(user))
                .thenReturn(user);
        when(conversionService.convert(user, CreateUserResponse.class))
                .thenReturn(userResponse);
        var response = userService.createUser(createUserRequest);

        assertThat(response).isNotNull();
        verify(userRepository).save(user);
        verify(userWorkspaceRepository).save(any(UserWorkspace.class));
        verify(workspaceService).createWorkspaceForNewUser(user, createUserRequest.getUsername());
    }
}