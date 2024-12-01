package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.UserRepository;
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

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest createWorkspaceRequest) {
        return conversionService.convert(createWorkspace(createWorkspaceRequest.getTelegramUserId(), createWorkspaceRequest.getName()), WorkspaceResponse.class);
    }

    @Override
    @Transactional
    public Workspace createWorkspace(Long telegramUserId, String name) {
        var user = userRepository.findByTelegramUserIdWithWorkspaces(telegramUserId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND,
                        MSG_USER_NOT_FOUND, telegramUserId)
        );

        var workspaceExists = user.getWorkspaces().stream()
                .anyMatch(workspace -> workspace.getName().equals(name));

        if (workspaceExists) {
            throw new ServiceException(ErrorType.CONFLICT, MSG_WORKSPACE_ALREADY_EXIST, name);
        }

        var workspace = new Workspace();
        workspace.setName(name);
        workspace.setUser(user);
        var savedWorkspace = workspaceRepository.save(workspace);
        s3Client.createFolder(user.getBucketName(), name);

        return savedWorkspace;
    }

    @Transactional
    @Override
    public Workspace createWorkspaceForNewUser(User user, String name) {
        var workspace = new Workspace();
        workspace.setName(name);
        workspace.setUser(user);

        var savedWorkspace = workspaceRepository.save(workspace);
        s3Client.createFolder(user.getBucketName(), name);

        return savedWorkspace;
    }

    @Override
    @Transactional(readOnly = true)
    public PageWorkspaceResponse getWorkspaces(Long telegramUserId, PageRequest pageRequest) {
        userRepository.findByTelegramUserId(telegramUserId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND,
                        MSG_USER_NOT_FOUND, telegramUserId)
        );
        var workspaces =  workspaceRepository.findAll(SpecificationUtils.getWorkspacesSpecification(telegramUserId), pageRequest);
        var pages = workspaces.map(workspace -> conversionService.convert(workspace, WorkspaceResponse.class));

        return conversionService.convert(pages, PageWorkspaceResponse.class);
    }

    @Override
    @Transactional
    public WorkspaceResponse changeWorkspaceName(UUID workspaceId, String newWorkspaceName) {
        var workspace = workspaceRepository.findById(workspaceId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_FOUND, workspaceId)
        );

        workspace.setName(newWorkspaceName);

        var updatedWorkspace = workspaceRepository.save(workspace);

        return conversionService.convert(updatedWorkspace, WorkspaceResponse.class);
    }

    public void deleteWorkspace(UUID workspaceId) {
        var workspace = workspaceRepository.findById(workspaceId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_WORKSPACE_NOT_FOUND, workspaceId)
        );

        if (workspace.getUser().getCurrentWorkspace().getId().equals(workspaceId)) {
            throw new ServiceException(ErrorType.CONFLICT, MSG_DELETE_CURRENT_WORKSPACE);
        }

        workspaceRepository.delete(workspace);
        s3Client.deleteFolder(workspace.getUser().getBucketName(), workspace.getName());
    }
}
