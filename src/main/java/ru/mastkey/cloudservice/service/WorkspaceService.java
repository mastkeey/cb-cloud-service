package ru.mastkey.cloudservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.UUID;

public interface WorkspaceService {
    Workspace createWorkspace(UUID userId, String name);

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest);

    PageWorkspaceResponse getWorkspaces(UUID userId, PageRequest pageRequest);

    WorkspaceResponse changeWorkspaceName(UUID workspaceId, UUID userId, String newName);

    void deleteWorkspace(UUID workspaceId, UUID userId);

    Workspace createWorkspaceForNewUser(User user, String name);
}
