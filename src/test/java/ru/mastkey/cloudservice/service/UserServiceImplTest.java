package ru.mastkey.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.security.JwtService;
import ru.mastkey.cloudservice.service.impl.UserServiceImpl;
import ru.mastkey.model.AuthUserRequest;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.TokenResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ConversionService conversionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWorkspaceRepository userWorkspaceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpContextService httpContextService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest createUserRequest;
    private AuthUserRequest authUserRequest;
    private User user;
    private CreateUserResponse userResponse;
    private UUID workspaceId;


    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setPassword("password");

        user = new User();
        user.setId(UUID.randomUUID());

        userResponse = new CreateUserResponse();
        userResponse.setId(user.getId());

        authUserRequest = new AuthUserRequest();
        authUserRequest.setUsername("testuser");
        authUserRequest.setPassword("password");

        workspaceId = UUID.randomUUID();
    }

    @Test
    void createUser_ShouldCreateUserSuccessfully() {
        when(conversionService.convert(createUserRequest, User.class))
                .thenReturn(user);
        when(userRepository.save(user))
                .thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        var response = userService.createUser(createUserRequest);

        assertThat(response).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void authUser_ShouldReturnTokenSuccessfully() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        when(jwtService.generateToken(user)).thenReturn("test_token");

        TokenResponse response = userService.auth(authUserRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("test_token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtService).generateToken(user);
    }

    @Test
    void authUser_ShouldThrowUnauthorized_WhenCredentialsAreInvalid() {
        doThrow(new ServiceException(ErrorType.UNAUTHORIZED, "Incorrect password"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> userService.auth(authUserRequest));

        assertThat(exception.getMessage()).isEqualTo("Incorrect password");
        assertThat(exception.getCode()).isEqualTo(ErrorType.UNAUTHORIZED.getCode());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void addNewWorkspaceById_ShouldAddWorkspaceSuccessfully() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.of(user));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(new Workspace().setName("Test Workspace").setId(workspaceId)));

        userService.addNewWorkspaceById(workspaceId);

        verify(userWorkspaceRepository).save(any(UserWorkspace.class));
    }

    @Test
    void addNewWorkspaceById_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> userService.addNewWorkspaceById(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void addNewWorkspaceById_ShouldThrowNotFound_WhenWorkspaceDoesNotExist() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.of(user));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> userService.addNewWorkspaceById(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void addNewWorkspaceById_ShouldThrowConflict_WhenWorkspaceAlreadyExistsForUser() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        Workspace existingWorkspace = new Workspace().setName("Test Workspace").setId(workspaceId);
        user.setWorkspaces(List.of(existingWorkspace));
        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.of(user));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> userService.addNewWorkspaceById(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }
}