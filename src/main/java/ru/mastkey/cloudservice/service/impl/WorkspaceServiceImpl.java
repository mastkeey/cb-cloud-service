package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.service.HttpContextService;
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.cloudservice.util.SpecificationUtils;
import ru.mastkey.model.ChangeWorkspaceNameRequest;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;
    private final ConversionService conversionService;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final HttpContextService httpContextService;

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest) {
        log.info("Creating workspace with name: {}", createWorkspaceRequest.getName());
        var workspace = createWorkspace(createWorkspaceRequest.getName());
        log.info("Workspace created successfully with ID: {}", workspace.getId());
        return conversionService.convert(workspace, WorkspaceResponse.class);
    }

    @Override
    public Workspace createWorkspace(String name) {
        log.info("Validating and creating workspace with name: {}", name);
        var userId = httpContextService.getUserIdFromJwtToken();
        var user = validateAndGetUser(userId);

        var workspaceExists = user.getWorkspaces().stream()
                .filter(workspace -> workspace.getOwner().getId().equals(user.getId()))
                .anyMatch(workspace -> workspace.getName().equals(name));

        if (workspaceExists) {
            log.warn("Workspace already exists with name: {}", name);
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, name);
        }

        var workspace = new Workspace();
        workspace.setName(name);
        workspace.setOwner(user);
        var savedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace saved in repository: {}", savedWorkspace.getId());

        s3Client.createFolder(user.getBucketName(), name);
        log.info("S3 folder created for workspace: {}", name);

        var userWorkspace = new UserWorkspace();
        userWorkspace.setUser(user);
        userWorkspace.setWorkspace(workspace);
        userWorkspaceRepository.save(userWorkspace);
        log.info("Workspace linked to user: {}", userId);

        return savedWorkspace;
    }

    @Override
    @Transactional(readOnly = true)
    public PageWorkspaceResponse getWorkspaces(PageRequest pageRequest) {
        log.info("Fetching workspaces for the current user");
        var userId = httpContextService.getUserIdFromJwtToken();
        validateAndGetUser(userId);

        var workspaces = workspaceRepository.findAll(SpecificationUtils.getWorkspacesSpecification(userId), pageRequest);
        log.debug("Found {} workspaces for user: {}", workspaces.getTotalElements(), userId);

        var pages = workspaces.map(workspace -> conversionService.convert(workspace, WorkspaceResponse.class));
        return conversionService.convert(pages, PageWorkspaceResponse.class);
    }

    @Override
    @Transactional
    public WorkspaceResponse changeWorkspaceName(UUID workspaceId, ChangeWorkspaceNameRequest changeWorkspaceNameRequest) {
        log.info("Changing workspace name for ID: {}", workspaceId);
        var newName = changeWorkspaceNameRequest.getName();
        var userId = httpContextService.getUserIdFromJwtToken();
        var user = validateAndGetUser(userId);

        var workspaceExists = user.getWorkspaces().stream()
                .anyMatch(workspace -> workspace.getName().equals(newName));

        if (workspaceExists) {
            log.warn("Workspace name already exists: {}", newName);
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, newName);
        }

        var workspace = user.getWorkspaces().stream()
                .filter(workspacee -> workspacee.getId().equals(workspaceId))
                .findFirst().orElseThrow(
                        () -> {
                            log.error("Workspace not found: {}", workspaceId);
                            return new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_FOUND, workspaceId);
                        });

        workspace.setName(newName);
        var updatedWorkspace = workspaceRepository.save(workspace);
        log.info("Workspace name updated successfully: {}", workspaceId);

        return conversionService.convert(updatedWorkspace, WorkspaceResponse.class);
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        log.info("Deleting workspace with ID: {}", workspaceId);
        var userId = httpContextService.getUserIdFromJwtToken();

        var user = validateAndGetUser(userId);

        var workspace = user.getWorkspaces().stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Workspace not linked to user: {}, ID: {}", userId, workspaceId);
                    return new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_LINKED_TO_USER, workspaceId, userId);
                });

        if (workspace.getOwner().getId().equals(userId)) {
            userWorkspaceRepository.deleteByWorkspaceId(workspaceId);
            workspaceRepository.delete(workspace);
            s3Client.deleteFolder(workspace.getOwner().getBucketName(), workspace.getName());
            log.info("Workspace and its S3 folder deleted: {}", workspaceId);
        } else {
            userWorkspaceRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);
            log.info("User unlinked from workspace: {}", workspaceId);
        }
    }

    @Override
    public List<WorkspaceResponse> getAllWorkspaces() {
        log.info("Fetching all workspaces for the current user");
        var userId = httpContextService.getUserIdFromJwtToken();
        var user = validateAndGetUser(userId);

        var workspaces = user.getWorkspaces().stream()
                .map(workspace -> conversionService.convert(workspace, WorkspaceResponse.class))
                .toList();

        log.info("Fetched {} workspaces for user: {}", workspaces.size(), userId);
        return workspaces;
    }

    private User validateAndGetUser(UUID userId) {
        return userRepository.findByUserIdWithWorkspaces(userId).orElseThrow(
                () -> {
                    log.error("User not found: {}", userId);
                    return new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, userId);
                });
    }
}