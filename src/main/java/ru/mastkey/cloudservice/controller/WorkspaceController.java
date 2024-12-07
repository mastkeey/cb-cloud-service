package ru.mastkey.cloudservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.mastkey.api.WorkspaceControllerApi;
import ru.mastkey.cloudservice.configuration.properties.Properties;
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.cloudservice.util.PaginationUtils;
import ru.mastkey.model.ChangeWorkspaceNameRequest;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WorkspaceController implements WorkspaceControllerApi {

    private final WorkspaceService workspaceService;
    private final Properties properties;

    @Override
    public ResponseEntity<WorkspaceResponse> changeWorkspaceName(UUID workspaceId, ChangeWorkspaceNameRequest changeWorkspaceNameRequest) {
        return ResponseEntity.ok(workspaceService.changeWorkspaceName(workspaceId, changeWorkspaceNameRequest));
    }

    @Override
    public ResponseEntity<WorkspaceResponse> createWorkspace(CreateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.createWorkspace(request));
    }

    @Override
    public ResponseEntity<Void> deleteWorkspace(UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces() {
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }

    @Override
    public ResponseEntity<PageWorkspaceResponse> getWorkspaces(Integer page, Integer pageSize) {
        var pageRequest = PaginationUtils.buildPageRequest(page, pageSize, properties.getPageSize());
        return ResponseEntity.ok(workspaceService.getWorkspaces(pageRequest));
    }
}
