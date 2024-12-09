package ru.mastkey.cloudservice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.PageFileResponse;

import java.util.List;
import java.util.UUID;

public interface FileService {
    void uploadFiles(UUID workspaceId, List<MultipartFile> files);
    void uploadFile(MultipartFile file, Workspace workspace, User user);
    void deleteFile(UUID fileId, UUID workspaceId);
    PageFileResponse getFilesInfo(UUID workspaceId, PageRequest pageRequest);
    FileContent downloadFile(UUID fileId, UUID workspaceId);
}
