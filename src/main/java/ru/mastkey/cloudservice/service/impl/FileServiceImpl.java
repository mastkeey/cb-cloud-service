package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.service.FileService;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.cloudservice.util.SpecificationUtils;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

import java.util.List;
import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final S3Client s3Client;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ConversionService conversionService;

    @Override
    @Transactional
    public void uploadFiles(List<MultipartFile> files, Long userId) {
        var user = userRepository.findByTelegramUserIdWithWorkspaces(userId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, userId)
        );

        var currentWorkspace = user.getCurrentWorkspace();

        files.forEach(file -> uploadFile(file, currentWorkspace));
    }

    @Override
    @Transactional
    public void uploadFile(MultipartFile file, Workspace workspace) {
        var originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_INVALID_NAME);
        }

        var fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(originalFileName);
        var fileExtension = FileUtils.getFileExtension(originalFileName);
        var bucketName = workspace.getUser().getBucketName();
        var relativePath = FileUtils.generateRelativePath(workspace.getName(), fileNameWithoutExtension, fileExtension);

        var existingFile = fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, fileNameWithoutExtension, fileExtension);

        if (existingFile.isPresent()) {
            s3Client.uploadFile(file, bucketName, existingFile.get().getPath());
        } else {
            var newFile = File.builder()
                    .workspace(workspace)
                    .fileName(fileNameWithoutExtension)
                    .fileExtension(fileExtension)
                    .path(relativePath)
                    .build();

            fileRepository.save(newFile);
            s3Client.uploadFile(file, bucketName, relativePath);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageFileResponse getFilesInfo(Long telegramUserId, PageRequest pageRequest) {
        var user = userRepository.findByTelegramUserIdWithWorkspaces(telegramUserId).orElseThrow(
                () -> new ServiceException(ErrorType.NOT_FOUND, MSG_USER_NOT_FOUND, telegramUserId)
        );

        var currentWorkspace = user.getCurrentWorkspace();

        var spec = SpecificationUtils.getFilesSpecification(currentWorkspace.getId());
        var files = fileRepository.findAll(spec, pageRequest);
        var pages = files.map(file -> conversionService.convert(file, FileResponse.class));
        return conversionService.convert(pages, PageFileResponse.class);
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        var file = fileRepository.findById(fileId).orElseThrow(
                () -> new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_NOT_FOUND, fileId)
        );

        var bucketName = file.getWorkspace().getUser().getBucketName();
        s3Client.deleteFile(bucketName, file.getPath());
        fileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent downloadFile(UUID fileId) {
        var file = fileRepository.findById(fileId).orElseThrow(
                () -> new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_NOT_FOUND, fileId)
        );

        var bucketName = file.getWorkspace().getUser().getBucketName();

        var fileStream = s3Client.getFileStream(bucketName, file.getPath());

        return new FileContent(fileStream, file);
    }
}