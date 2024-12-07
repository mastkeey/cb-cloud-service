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
import ru.mastkey.model.ChangeWorkspaceNameRequest;
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

    @Mock
    private HttpContextService httpContextService;

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
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));
        when(workspaceRepository.save(any(Workspace.class)))
                .thenReturn(workspace);
        when(conversionService.convert(workspace, WorkspaceResponse.class))
                .thenReturn(workspaceResponse);

        var response = workspaceService.createWorkspace(createWorkspaceRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(createWorkspaceRequest.getName());

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(user.getId());
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
        verify(s3Client, times(1)).createFolder(user.getBucketName(), createWorkspaceRequest.getName());
        verify(userWorkspaceRepository, times(1)).save(any(UserWorkspace.class));
    }


    @Test
    void createWorkspace_ShouldThrowConflictException_WhenWorkspaceExists() {
        user.getWorkspaces().add(workspace);
        workspace.setOwnerId(user.getId());
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace %s already exist".formatted(createWorkspaceRequest.getName()));

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(user.getId());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createFolder(anyString(), anyString());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void createWorkspace_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.createWorkspace(createWorkspaceRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(user.getId()));

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(user.getId());
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(s3Client, never()).createFolder(anyString(), anyString());
        verify(userWorkspaceRepository, never()).save(any(UserWorkspace.class));
    }

    @Test
    void getWorkspaces_ShouldThrowNotFoundException_WhenUserNotFound() {
        var pageRequest = PageRequest.of(0, 10);
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.getWorkspaces(pageRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(user.getId()));

        verify(userRepository, times(1)).findByUserIdWithWorkspaces(user.getId());
    }

    @Test
    void changeWorkspaceName_ShouldUpdateWorkspaceNameSuccessfully() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "new_name";
        var request = new ChangeWorkspaceNameRequest(newWorkspaceName);

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
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        user.getWorkspaces().add(workspace);

        when(workspaceRepository.save(workspace)).thenReturn(updatedWorkspace);

        when(conversionService.convert(updatedWorkspace, WorkspaceResponse.class))
                .thenReturn(updatedWorkspaceResponse);

        var result = workspaceService.changeWorkspaceName(workspaceId, request);

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
        var request = new ChangeWorkspaceNameRequest(newWorkspaceName);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, request));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace with id %s not found".formatted(workspaceId));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void changeWorkspaceName_ShouldThrowNotFoundException_WhenUserNotFound() {
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var newWorkspaceName = "new_name";
        var request = new ChangeWorkspaceNameRequest(newWorkspaceName);


        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, request));

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
        var request = new ChangeWorkspaceNameRequest(newWorkspaceName);


        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);

        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));

        var existingWorkspace = new Workspace();
        existingWorkspace.setName(newWorkspaceName);
        existingWorkspace.setOwnerId(user.getId());
        user.getWorkspaces().add(existingWorkspace);

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.changeWorkspaceName(workspaceId, request));

        assertThat(exception.getCode()).isEqualTo(ErrorType.CONFLICT.getCode());
        assertThat(exception.getMessage()).isEqualTo("Workspace %s already exist".formatted(newWorkspaceName));

        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void deleteWorkspace_ShouldDeleteWorkspaceSuccessfully() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));
        workspace.setOwnerId(user.getId());
        user.getWorkspaces().add(workspace);

        workspaceService.deleteWorkspace(workspace.getId());

        verify(userWorkspaceRepository, times(1)).deleteByWorkspaceId(workspace.getId());
        verify(workspaceRepository, times(1)).delete(workspace);
        verify(s3Client, times(1)).deleteFolder(user.getBucketName(), workspace.getName());
    }

    @Test
    void deleteWorkspace_ShouldThrowNotFoundException_WhenWorkspaceNotLinked() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        var idToDelete = UUID.randomUUID();
        when(userRepository.findByUserIdWithWorkspaces(user.getId()))
                .thenReturn(Optional.of(user));

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.deleteWorkspace(idToDelete));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).contains("not linked to user");

        verify(userWorkspaceRepository, never()).deleteByWorkspaceId(any());
        verify(workspaceRepository, never()).deleteById(idToDelete);
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }

    @Test
    void deleteWorkspace_ShouldThrowNotFoundException_WhenUserNotFound() {
        var workspaceId = UUID.randomUUID();
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> workspaceService.deleteWorkspace(workspaceId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(user.getId()));

        verify(userWorkspaceRepository, never()).deleteByWorkspaceId(any());
        verify(workspaceRepository, never()).deleteById(any());
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }

    @Test
    void deleteWorkspace_ShouldDeleteUserWorkspaceLink_WhenUserIsNotOwner() {
        var workspaceId = UUID.randomUUID();
        workspace.setId(workspaceId);
        workspace.setOwnerId(UUID.randomUUID());
        user.getWorkspaces().add(workspace);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userRepository.findByUserIdWithWorkspaces(user.getId())).thenReturn(Optional.of(user));

        workspaceService.deleteWorkspace(workspaceId);

        verify(userWorkspaceRepository).deleteByUserIdAndWorkspaceId(user.getId(), workspaceId);
        verify(workspaceRepository, never()).deleteById(any());
        verify(s3Client, never()).deleteFolder(anyString(), anyString());
    }

    @Test
    void getAllWorkspaces_ShouldReturnListOfWorkspacesSuccessfully() {
        var userId = UUID.randomUUID();
        var workspace1 = new Workspace();
        workspace1.setId(UUID.randomUUID());
        workspace1.setName("Workspace 1");

        var workspace2 = new Workspace();
        workspace2.setId(UUID.randomUUID());
        workspace2.setName("Workspace 2");

        var workspaceResponse1 = new WorkspaceResponse();
        workspaceResponse1.setName("Workspace 1");

        var workspaceResponse2 = new WorkspaceResponse();
        workspaceResponse2.setName("Workspace 2");

        user.setId(userId);
        user.getWorkspaces().add(workspace1);
        user.getWorkspaces().add(workspace2);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);
        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.of(user));
        when(conversionService.convert(workspace1, WorkspaceResponse.class)).thenReturn(workspaceResponse1);
        when(conversionService.convert(workspace2, WorkspaceResponse.class)).thenReturn(workspaceResponse2);

        var result = workspaceService.getAllWorkspaces();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(WorkspaceResponse::getName).containsExactlyInAnyOrder("Workspace 1", "Workspace 2");

        verify(httpContextService, times(1)).getUserIdFromJwtToken();
        verify(userRepository, times(1)).findByUserIdWithWorkspaces(userId);
        verify(conversionService, times(2)).convert(any(Workspace.class), eq(WorkspaceResponse.class));
    }

    @Test
    void getAllWorkspaces_ShouldThrowNotFoundException_WhenUserNotFound() {
        var userId = UUID.randomUUID();
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(userId);
        when(userRepository.findByUserIdWithWorkspaces(userId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> workspaceService.getAllWorkspaces());

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo("User with id %s not found".formatted(userId));

        verify(httpContextService, times(1)).getUserIdFromJwtToken();
        verify(userRepository, times(1)).findByUserIdWithWorkspaces(userId);
        verify(conversionService, never()).convert(any(Workspace.class), eq(WorkspaceResponse.class));
    }
}