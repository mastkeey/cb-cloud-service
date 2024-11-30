package ru.mastkey.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.service.impl.WorkspaceServiceImpl;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.WorkspaceResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Client s3Client;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private CreateWorkspaceRequest createWorkspaceRequest;
    private User user;
    private Workspace workspace;
    private WorkspaceResponse workspaceResponse;

    @BeforeEach
    void setUp() {
        var testTgUserId = 12345L;
        var testWorkspaceName = "test_workspace";

        createWorkspaceRequest = new CreateWorkspaceRequest();
        createWorkspaceRequest.setTelegramUserId(testTgUserId);
        createWorkspaceRequest.setName(testWorkspaceName);

        user = new User();
        user.setTelegramUserId(testTgUserId);
        user.setBucketName("test_bucket");

        workspace = new Workspace();
        workspace.setName(testWorkspaceName);
        workspace.setUser(user);
        workspace.setId(UUID.randomUUID());

        workspaceResponse = new WorkspaceResponse();
        workspaceResponse.setName(testWorkspaceName);
    }

    @Test
    void createWorkspace_ShouldCreateWorkspaceSuccessfully() {
        when(userRepository.findByTelegramUserIdWithWorkspaces(createWorkspaceRequest.getTelegramUserId()))
                .thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(Workspace.class)))
                .thenReturn(workspace);
        when(conversionService.convert(any(Workspace.class), eq(WorkspaceResponse.class)))
                .thenReturn(workspaceResponse);

        WorkspaceResponse response = workspaceService.createWorkspace(createWorkspaceRequest);

        assertThat(response).isNotNull();
        assertThat(createWorkspaceRequest.getName()).isEqualTo(response.getName());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(s3Client).createFolder(anyString(), anyString());
    }

    @Test
    void createWorkspace_ShouldThrowNotFoundException_WhenUserNotFound() {
        var testUser = new User();
        var testWorkspace = new Workspace();
        testWorkspace.setName(createWorkspaceRequest.getName());
        testUser.getWorkspaces().add(testWorkspace);

        when(userRepository.findByTelegramUserIdWithWorkspaces(createWorkspaceRequest.getTelegramUserId()))
                .thenReturn(Optional.of(testUser));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(ErrorType.CONFLICT.getCode()).isEqualTo(exception.getCode());
        assertThat("Workspace with name %s already exist".formatted(createWorkspaceRequest.getName())).isEqualTo(exception.getMessage());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createBucketIfNotExists(anyString());
    }

    @Test
    void createWorkspace_ShouldThrowNotFoundException_WorkspaceAlreadyExists() {
        when(userRepository.findByTelegramUserIdWithWorkspaces(createWorkspaceRequest.getTelegramUserId()))
                .thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(ErrorType.NOT_FOUND.getCode()).isEqualTo(exception.getCode());
        assertThat("User with id %s not found".formatted(createWorkspaceRequest.getTelegramUserId())).isEqualTo(exception.getMessage());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createBucketIfNotExists(anyString());
    }

    @Test
    void createWorkspace_WithUserIdAndName_ShouldCreateWorkspaceSuccessfully() {
        when(userRepository.findByTelegramUserIdWithWorkspaces(12345L))
                .thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(Workspace.class)))
                .thenReturn(workspace);

        Workspace response = workspaceService.createWorkspace(12345L, "test_workspace");

        assertThat(response).isNotNull();
        assertThat("test_workspace").isEqualTo(response.getName());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(s3Client).createFolder(anyString(),anyString());
    }

    @Test
    void getWorkspaces_ShouldThrowNotFoundException_WhenUserNotFound() {
        var telegramUserId = 12345L;
        var pageRequest = PageRequest.of(0, 10);

        when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.getWorkspaces(telegramUserId, pageRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(telegramUserId));
        verify(userRepository).findByTelegramUserId(telegramUserId);
    }

    @Test
    void changeWorkspaceName_ShouldUpdateWorkspaceNameSuccessfully() {
        var workspaceId = UUID.randomUUID();
        var newWorkspaceName = "new_name";
        var updatedWorkspace = new Workspace();
        updatedWorkspace.setId(workspaceId);
        updatedWorkspace.setName(newWorkspaceName);
        var updatedWorkspaceResponse = new WorkspaceResponse();
        updatedWorkspaceResponse.setName(newWorkspaceName);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(updatedWorkspace);
        when(conversionService.convert(any(Workspace.class), eq(WorkspaceResponse.class))).thenReturn(updatedWorkspaceResponse);

        var result = workspaceService.changeWorkspaceName(workspaceId, newWorkspaceName);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(newWorkspaceName);
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository).save(workspace);
        verify(conversionService).convert(updatedWorkspace, WorkspaceResponse.class);
    }

    @Test
    void changeWorkspaceName_ShouldThrowNotFoundException_WhenWorkspaceNotFound() {
        var workspaceId = UUID.randomUUID();
        var newWorkspaceName = "new_name";

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, newWorkspaceName));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with id %s not found".formatted(workspaceId));
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    void deleteWorkspace_ShouldDeleteWorkspaceSuccessfully() {
        var workspaceId = UUID.randomUUID();
        workspace.setId(workspaceId);
        user.setCurrentWorkspace(new Workspace().setId(UUID.randomUUID()));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        workspaceService.deleteWorkspace(workspaceId);

        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository).delete(workspace);
        verify(s3Client).deleteFolder(workspace.getUser().getBucketName(), workspace.getName());
    }

    @Test
    void deleteWorkspace_ShouldThrowNotFoundException_WhenWorkspaceNotFound() {
        var workspaceId = UUID.randomUUID();

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.deleteWorkspace(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with id %s not found".formatted(workspaceId));
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository, never()).delete(any(Workspace.class));
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }

    @Test
    void deleteWorkspace_ShouldThrowConflictException_WhenWorkspaceIsCurrent() {
        var workspaceId = UUID.randomUUID();
        var testUser = new User();
        var testWorkspace = new Workspace();
        testWorkspace.setId(workspaceId);
        testUser.setCurrentWorkspace(testWorkspace);
        testWorkspace.setUser(testUser);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(testWorkspace));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> workspaceService.deleteWorkspace(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        assertThat(exception.getMessage()).isEqualTo("You can't delete current workspace");
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository, never()).delete(any(Workspace.class));
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }
}