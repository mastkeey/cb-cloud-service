package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
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
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.cloudservice.util.SpecificationUtils;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;
    private final ConversionService conversionService;
    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest) {
        return conversionService.convert(createWorkspace(createWorkspaceRequest.getUserId(), createWorkspaceRequest.getName()), WorkspaceResponse.class);
    }

    @Override
    @Transactional
    public Workspace createWorkspace(UUID userId, String name) {
        var user = userRepository.findByUserIdWithWorkspaces(userId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND,
                        MSG_USER_NOT_FOUND, userId)
        );

        var workspaceExists = user.getWorkspaces().stream()
                .filter(workspace -> workspace.getOwnerId().equals(user.getId()))
                .anyMatch(workspace -> workspace.getName().equals(name));

        if (workspaceExists) {
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, name);
        }

        var workspace = new Workspace();
        workspace.setName(name);
        var savedWorkspace = workspaceRepository.save(workspace);
        s3Client.createFolder(user.getBucketName(), name);

        var userWorkspace = new UserWorkspace();
        userWorkspace.setUser(user);
        userWorkspace.setWorkspace(workspace);
        userWorkspaceRepository.save(userWorkspace);

        return savedWorkspace;
    }

    @Transactional
    @Override
    public Workspace createWorkspaceForNewUser(User user, String name) {
        var workspace = new Workspace();
        workspace.setName(name);
        workspace.setOwnerId(user.getId());

        var savedWorkspace = workspaceRepository.save(workspace);
        s3Client.createFolder(user.getBucketName(), name);

        return savedWorkspace;
    }

    @Override
    @Transactional(readOnly = true)
    public PageWorkspaceResponse getWorkspaces(UUID userId, PageRequest pageRequest) {
        userRepository.findById(userId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND,
                        MSG_USER_NOT_FOUND, userId)
        );
        var workspaces = workspaceRepository.findAll(SpecificationUtils.getWorkspacesSpecification(userId), pageRequest);
        var pages = workspaces.map(workspace -> conversionService.convert(workspace, WorkspaceResponse.class));

        return conversionService.convert(pages, PageWorkspaceResponse.class);
    }

    @Override
    @Transactional
    public WorkspaceResponse changeWorkspaceName(UUID workspaceId, UUID userId, String newName) {
        var user = userRepository.findByUserIdWithWorkspaces(userId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND,
                        MSG_USER_NOT_FOUND, userId)
        );

        var usersWorkspace = user.getWorkspaces().stream()
                .filter(workspace -> workspace.getOwnerId().equals(userId))
                .toList();

        var workspaceExists = user.getWorkspaces().stream()
                .anyMatch(workspace -> workspace.getName().equals(newName));

        if (workspaceExists) {
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, newName);
        }

        var workspace = usersWorkspace.stream()
                .filter(workspacee -> workspacee.getId().equals(workspaceId))
                .findFirst().orElseThrow(
                        () -> new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_FOUND, workspaceId)
                );

        workspace.setName(newName);

        var updatedWorkspace = workspaceRepository.save(workspace);

        return conversionService.convert(updatedWorkspace, WorkspaceResponse.class);
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID workspaceId, UUID userId) {
        var user = userRepository.findByUserIdWithWorkspaces(userId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, userId)
        );

        var workspace = user.getWorkspaces().stream()
                .filter(w -> w.getId().equals(workspaceId))
                .findFirst()
                .orElseThrow(() -> new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_LINKED_TO_USER, userId, workspaceId));

        if (workspace.getOwnerId().equals(userId)) {
            userWorkspaceRepository.deleteByWorkspaceId(workspaceId);
            workspaceRepository.delete(workspace);
            s3Client.deleteFolder(user.getBucketName(), workspace.getName());
        } else {
            userWorkspaceRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);
        }
    }
}
