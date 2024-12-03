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
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.service.FileService;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.cloudservice.util.SpecificationUtils;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final S3Client s3Client;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ConversionService conversionService;
    private final UserWorkspaceRepository userWorkspaceRepository;

    @Override
    @Transactional
    public void uploadFiles(UUID userId, UUID workspaceId, List<MultipartFile> files) {
        var userWorkspace = validateUserWorkspace(userId, workspaceId);
        files.forEach(file -> uploadFile(file, userWorkspace.getWorkspace(), userWorkspace.getUser()));
    }

    @Override
    @Transactional
    public void uploadFile(MultipartFile file, Workspace workspace, User user) {
        var relativePath = validateAndGeneratePath(file, workspace);
        var existingFile = findExistingFile(workspace,
                FileUtils.getFileNameWithoutExtension(file.getOriginalFilename()),
                FileUtils.getFileExtension(file.getOriginalFilename()));

        if (existingFile.isPresent()) {
            uploadToS3(file, user.getBucketName(), existingFile.get().getPath());
        } else {
            var newFile = File.builder()
                    .workspace(workspace)
                    .fileName(FileUtils.getFileNameWithoutExtension(file.getOriginalFilename()))
                    .fileExtension(FileUtils.getFileExtension(file.getOriginalFilename()))
                    .path(relativePath)
                    .build();

            uploadToS3(file, user.getBucketName(), relativePath);
            fileRepository.save(newFile);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageFileResponse getFilesInfo(UUID userId, UUID workspaceId, PageRequest pageRequest) {
        validateUserWorkspace(userId, workspaceId);
        var spec = SpecificationUtils.getFilesSpecification(workspaceId);
        var files = fileRepository.findAll(spec, pageRequest);
        var pages = files.map(file -> conversionService.convert(file, FileResponse.class));
        return conversionService.convert(pages, PageFileResponse.class);
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId, UUID workspaceId, UUID userId) {
        var file = validateFile(fileId, workspaceId);
        var userWorkspace = validateUserWorkspace(userId, workspaceId);
        s3Client.deleteFile(getBucketName(userWorkspace), file.getPath());
        fileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent downloadFile(UUID fileId, UUID workspaceId, UUID userId) {
        var file = validateFile(fileId, workspaceId);
        var userWorkspace = validateUserWorkspace(userId, workspaceId);
        var fileStream = s3Client.getFileStream(getBucketName(userWorkspace), file.getPath());
        return new FileContent(fileStream, file);
    }

    private UserWorkspace validateUserWorkspace(UUID userId, UUID workspaceId) {
        return userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ServiceException(ErrorType.FORBIDDEN, MSG_WORKSPACE_NOT_LINKED_TO_USER, workspaceId, userId));
    }

    private File validateFile(UUID fileId, UUID workspaceId) {
        var file = fileRepository.findById(fileId).orElseThrow(
                () -> new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_NOT_FOUND, fileId)
        );
        if (!file.getWorkspace().getId().equals(workspaceId)) {
            throw new ServiceException(ErrorType.FORBIDDEN, MSG_FILE_NOT_IN_WORKSPACE, fileId, workspaceId);
        }
        return file;
    }

    private String validateAndGeneratePath(MultipartFile file, Workspace workspace) {
        var originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_INVALID_NAME);
        }
        var fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(originalFileName);
        var fileExtension = FileUtils.getFileExtension(originalFileName);
        return FileUtils.generateRelativePath(workspace.getName(), fileNameWithoutExtension, fileExtension);
    }

    private Optional<File> findExistingFile(Workspace workspace, String fileNameWithoutExtension, String fileExtension) {
        return fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, fileNameWithoutExtension, fileExtension);
    }

    private void uploadToS3(MultipartFile file, String bucketName, String path) {
        s3Client.uploadFile(file, bucketName, path);
    }

    private String getBucketName(UserWorkspace userWorkspace) {
        return userWorkspace.getUser().getBucketName();
    }
}