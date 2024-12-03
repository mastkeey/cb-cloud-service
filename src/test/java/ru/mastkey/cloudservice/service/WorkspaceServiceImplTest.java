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
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
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

    @Mock
    private UserWorkspaceRepository userWorkspaceRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private CreateWorkspaceRequest createWorkspaceRequest;
    private User user;
    private Workspace workspace;
    private WorkspaceResponse workspaceResponse;
    private UserWorkspace userWorkspace;

    @BeforeEach
    void setUp() {
        var testUserId = UUID.randomUUID();
        var testWorkspaceName = "test_workspace";

        createWorkspaceRequest = new CreateWorkspaceRequest();
        createWorkspaceRequest.setUserId(testUserId);
        createWorkspaceRequest.setName(testWorkspaceName);

        user = new User();
        user.setId(testUserId);
        user.setBucketName("test_bucket");

        workspace = new Workspace();
        workspace.setName(testWorkspaceName);
        workspace.setId(UUID.randomUUID());

        workspaceResponse = new WorkspaceResponse();
        workspaceResponse.setName(testWorkspaceName);

        userWorkspace = new UserWorkspace();
        userWorkspace.setWorkspace(workspace);
        userWorkspace.setUser(user);
    }

    @Test
    void createWorkspace_ShouldCreateWorkspaceSuccessfully() {
        when(userRepository.findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId()))
                .thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(Workspace.class)))
                .thenReturn(workspace);
        when(conversionService.convert(workspace, WorkspaceResponse.class))
                .thenReturn(workspaceResponse);

        var response = workspaceService.createWorkspace(createWorkspaceRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(createWorkspaceRequest.getName());

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId());
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
        verify(s3Client, times(1)).createFolder(user.getBucketName(), createWorkspaceRequest.getName());
        verify(userWorkspaceRepository, times(1)).save(any(UserWorkspace.class));
    }


    @Test
    void createWorkspace_ShouldThrowConflictException_WhenWorkspaceExists() {
        user.getWorkspaces().add(workspace);
        workspace.setOwnerId(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId()))
                .thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with name %s already exist".formatted(createWorkspaceRequest.getName()));

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createFolder(anyString(), anyString());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void createWorkspace_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(createWorkspaceRequest.getUserId()));

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(createWorkspaceRequest.getUserId());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createFolder(anyString(), anyString());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void getWorkspaces_ShouldThrowNotFoundException_WhenUserNotFound() {
        var pageRequest = PageRequest.of(0, 10);

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.getWorkspaces(user.getId(), pageRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(user.getId()));

        verify(userRepository, times(1)).findById(user.getId());
    }

    @Test
    void changeWorkspaceName_ShouldUpdateWorkspaceNameSuccessfully() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "new_name";

        var user = new User();
        user.setId(userId);

        var workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setOwnerId(userId);
        workspace.setName("old_name");

        var updatedWorkspace = new Workspace();
        updatedWorkspace.setId(workspaceId);
        updatedWorkspace.setName(newWorkspaceName);
        updatedWorkspace.setOwnerId(userId);

        var updatedWorkspaceResponse = new WorkspaceResponse()
                .workspaceId(workspaceId)
                .name(newWorkspaceName);

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        user.getWorkspaces().add(workspace);

        when(workspaceRepository.save(workspace)).thenReturn(updatedWorkspace);

        when(conversionService.convert(updatedWorkspace, WorkspaceResponse.class))
                .thenReturn(updatedWorkspaceResponse);

        var result = workspaceService.changeWorkspaceName(workspaceId, userId, newWorkspaceName);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(newWorkspaceName);

        verify(userRepository).findByUserIdWithWorkspaces(userId);
        verify(workspaceRepository).save(workspace);
        verify(conversionService).convert(updatedWorkspace, WorkspaceResponse.class);
    }

    @Test
    void changeWorkspaceName_ShouldThrowNotFoundException_WhenWorkspaceNotFound() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "new_name";

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, userId, newWorkspaceName));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with id %s not found".formatted(workspaceId));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void changeWorkspaceName_ShouldThrowNotFoundException_WhenUserNotFound() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "new_name";

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, userId, newWorkspaceName));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(userId));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void changeWorkspaceName_ShouldThrowConflictException_WhenWorkspaceNameAlreadyExists() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "existing_name";
        workspace.setName(newWorkspaceName);
        workspace.setOwnerId(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        var existingWorkspace = new Workspace();
        existingWorkspace.setName(newWorkspaceName);
        existingWorkspace.setOwnerId(user.getId());
        user.getWorkspaces().add(existingWorkspace);

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, userId, newWorkspaceName));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with name %s already exist".formatted(newWorkspaceName));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void deleteWorkspace_ShouldDeleteWorkspaceSuccessfully() {
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));
        workspace.setOwnerId(user.getId());
        user.getWorkspaces().add(workspace);

        workspaceService.deleteWorkspace(workspace.getId(), user.getId());

        verify(userWorkspaceRepository, times(1)).deleteByWorkspaceId(workspace.getId());
        verify(workspaceRepository, times(1)).delete(workspace);
        verify(s3Client, times(1)).deleteFolder(user.getBucketName(), workspace.getName());
    }

    @Test
    void deleteWorkspace_ShouldThrowNotFoundException_WhenWorkspaceNotLinked() {
        var idToDelete = UUID.randomUUID();
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.deleteWorkspace(idToDelete, user.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).contains("not linked to user");

        verify(userWorkspaceRepository, never()).deleteByWorkspaceId(any());
        verify(workspaceRepository, never()).deleteById(idToDelete);
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }
}