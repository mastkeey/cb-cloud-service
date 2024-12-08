package ru.mastkey.cloudservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.api.FileControllerApi;
import ru.mastkey.cloudservice.configuration.properties.Properties;
import ru.mastkey.cloudservice.service.FileService;
import ru.mastkey.cloudservice.util.PaginationUtils;
import ru.mastkey.cloudservice.util.ResponseFactory;
import ru.mastkey.model.PageFileResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController implements FileControllerApi {

    private final FileService fileService;
    private final Properties properties;

    @Override
    public ResponseEntity<Void> deleteFile(UUID fileId, UUID workspaceId) {
        fileService.deleteFile(fileId, workspaceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getFile(UUID fileId, UUID workspaceId) {
        return ResponseFactory.createFileResponse(fileService.downloadFile(fileId, workspaceId));
    }

    @Override
    public ResponseEntity<PageFileResponse> getFilesInfo(UUID workspaceId, Integer page, Integer pageSize) {
        var pageRequest = PaginationUtils.buildPageRequest(page, pageSize, properties.getPageSize());
        return ResponseEntity.ok(fileService.getFilesInfo(workspaceId, pageRequest));
    }

    @Override
    public ResponseEntity<Void> uploadFiles(UUID workspaceId, List<MultipartFile> files) {
        fileService.uploadFiles(workspaceId, files);
        return ResponseEntity.ok().build();
    }
}
