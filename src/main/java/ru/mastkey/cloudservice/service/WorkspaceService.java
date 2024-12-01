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
    Workspace createWorkspace(Long userId, String name);

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest);

    PageWorkspaceResponse getWorkspaces(Long telegramUserId, PageRequest pageRequest);

    WorkspaceResponse changeWorkspaceName(UUID workspaceId, String newWorkspaceName);

    void deleteWorkspace(UUID workspaceId);

    Workspace createWorkspaceForNewUser(User user, String name);
}
