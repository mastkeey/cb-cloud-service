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
import ru.mastkey.model.FileResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController implements FileControllerApi {

    private final FileService fileService;
    private final Properties properties;

    @Override
    public ResponseEntity<Void> deleteFile(UUID fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getFile(UUID fileId) {
        return ResponseFactory.createFileResponse(fileService.downloadFile(fileId));
    }

    @Override
    public ResponseEntity<List<FileResponse>> getFilesInfo(Long telegramUserId, Integer pageNumber, Integer pageSize) {
        var pageRequest = PaginationUtils.buildPageRequest(pageNumber, pageSize, properties.getPageSize());
        return ResponseFactory.buildPagedResponse(fileService.getFilesInfo(telegramUserId, pageRequest));
    }

    @Override
    public ResponseEntity<Void> uploadFiles(Long telegramUserId, List<MultipartFile> files) {
        fileService.uploadFiles(files, telegramUserId);
        return ResponseEntity.ok().build();
    }
}
