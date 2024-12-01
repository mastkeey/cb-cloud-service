package ru.mastkey.cloudservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.mastkey.api.WorkspaceControllerApi;
import ru.mastkey.cloudservice.configuration.properties.Properties;
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.cloudservice.util.PaginationUtils;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WorkspaceController implements WorkspaceControllerApi {

    private final WorkspaceService workspaceService;
    private final Properties properties;

    @Override
    public ResponseEntity<WorkspaceResponse> createWorkspace(CreateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.createWorkspace(request));
    }

    @Override
    public ResponseEntity<WorkspaceResponse> changeWorkspaceName(
            UUID workspaceId,
            String newWorkspaceName) {
        return ResponseEntity.ok(workspaceService.changeWorkspaceName(workspaceId, newWorkspaceName));
    }

    @Override
    public ResponseEntity<Void> deleteWorkspace(UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PageWorkspaceResponse> getWorkspaces(Long telegramUserId, Integer pageNumber, Integer pageSize) {
        var pageRequest = PaginationUtils.buildPageRequest(pageNumber, pageSize, properties.getPageSize());
        return ResponseEntity.ok(workspaceService.getWorkspaces(telegramUserId, pageRequest));
    }
}
