package ru.mastkey.cloudservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.FileResponse;

import java.util.List;
import java.util.UUID;

public interface FileService {
    void uploadFiles(List<MultipartFile> files, Long userId);
    void uploadFile(MultipartFile file, Workspace workspace);
    void deleteFile(UUID fileId);
    Page<FileResponse> getFilesInfo(Long telegramUserId, PageRequest pageRequest);
    FileContent downloadFile(UUID fileId);
}
