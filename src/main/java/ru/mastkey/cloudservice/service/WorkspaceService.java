package ru.mastkey.cloudservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.ChangeWorkspaceNameRequest;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
    Workspace createWorkspace(String name);

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest);

    PageWorkspaceResponse getWorkspaces(PageRequest pageRequest);

    WorkspaceResponse changeWorkspaceName(UUID workspaceId, ChangeWorkspaceNameRequest changeWorkspaceNameRequest);

    void deleteWorkspace(UUID workspaceId);

    List<WorkspaceResponse> getAllWorkspaces();

}
