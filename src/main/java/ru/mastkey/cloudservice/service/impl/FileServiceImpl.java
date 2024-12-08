package ru.mastkey.cloudservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.service.FileService;
import ru.mastkey.cloudservice.service.HttpContextService;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.cloudservice.util.SpecificationUtils;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ru.mastkey.cloudservice.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3Client;
    private final FileRepository fileRepository;
    private final ConversionService conversionService;
    private final UserWorkspaceRepository userWorkspaceRepository;
    private final HttpContextService httpContextService;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public void uploadFiles(UUID workspaceId, List<MultipartFile> files) {
        log.info("Uploading multiple files to workspace: {}", workspaceId);
        var userId = httpContextService.getUserIdFromJwtToken();
        var userWorkspace = validateUserWorkspace(userId, workspaceId);

        files.forEach(file -> {
            log.debug("Uploading file: {} to workspace: {}", file.getOriginalFilename(), workspaceId);
            uploadFile(file, userWorkspace.getWorkspace(), userWorkspace.getUser());
        });

        log.info("Finished uploading files to workspace: {}", workspaceId);
    }

    @Override
    @Transactional
    public void uploadFile(MultipartFile file, Workspace workspace, User user) {
        log.info("Uploading file: {} to workspace: {}", file.getOriginalFilename(), workspace.getId());

        var relativePath = validateAndGeneratePath(file, workspace);
        log.debug("Generated relative path: {}", relativePath);

        log.info("file original name: {}", file.getOriginalFilename());
        log.info("file name: {}", file.getName());

        var existingFile = findExistingFile(workspace,
                FileUtils.getFileNameWithoutExtension(file.getOriginalFilename()),
                FileUtils.getFileExtension(file.getOriginalFilename()));

        if (existingFile.isPresent()) {
            log.warn("File already exists, overwriting: {}", existingFile.get().getPath());
            uploadToS3(file, workspace.getOwner().getBucketName(), existingFile.get().getPath());
        } else {
            log.debug("File does not exist, creating new file entry");
            var newFile = File.builder()
                    .workspace(workspace)
                    .fileName(FileUtils.getFileNameWithoutExtension(file.getOriginalFilename()))
                    .fileExtension(FileUtils.getFileExtension(file.getOriginalFilename()))
                    .path(relativePath)
                    .build();

            uploadToS3(file, user.getBucketName(), relativePath);
            fileRepository.save(newFile);
            log.info("File successfully uploaded and saved: {}", relativePath);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageFileResponse getFilesInfo(UUID workspaceId, PageRequest pageRequest) {
        log.info("Fetching files info for workspace: {}", workspaceId);
        var userId = httpContextService.getUserIdFromJwtToken();
        validateUserWorkspace(userId, workspaceId);

        var spec = SpecificationUtils.getFilesSpecification(workspaceId);
        var files = fileRepository.findAll(spec, pageRequest);

        log.debug("Fetched files for workspace: {}, total: {}", workspaceId, files.getTotalElements());
        var pages = files.map(file -> conversionService.convert(file, FileResponse.class));

        log.info("Returning paginated file response for workspace: {}", workspaceId);
        return conversionService.convert(pages, PageFileResponse.class);
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId, UUID workspaceId) {
        log.info("Deleting file: {} from workspace: {}", fileId, workspaceId);
        var userId = httpContextService.getUserIdFromJwtToken();

        var file = validateFile(fileId, workspaceId);
        var userWorkspace = validateUserWorkspace(userId, workspaceId);

        var bucketName = userWorkspace.getWorkspace().getOwner().getBucketName();

        log.debug("Deleting file from S3 bucket: {}, path: {}", userWorkspace.getUser().getBucketName(), file.getPath());
        s3Client.deleteFile(bucketName, file.getPath());
        fileRepository.delete(file);

        log.info("File successfully deleted: {} from workspace: {}", fileId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent downloadFile(UUID fileId, UUID workspaceId) {
        log.info("Downloading file: {} from workspace: {}", fileId, workspaceId);
        var userId = httpContextService.getUserIdFromJwtToken();

        var file = validateFile(fileId, workspaceId);
        var userWorkspace = validateUserWorkspace(userId, workspaceId);

        var bucketName = userWorkspace.getWorkspace().getOwner().getBucketName();

        log.debug("Fetching file stream from S3 bucket: {}, path: {}", userWorkspace.getUser().getBucketName(), file.getPath());
        var fileStream = s3Client.getFileStream(bucketName, file.getPath());

        log.info("File successfully downloaded: {} from workspace: {}", fileId, workspaceId);
        return new FileContent(fileStream, file);
    }

    private UserWorkspace validateUserWorkspace(UUID userId, UUID workspaceId) {
        log.debug("Validating user workspace: userId={}, workspaceId={}", userId, workspaceId);
        return userWorkspaceRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> {
                    log.error("User is not linked to workspace: userId={}, workspaceId={}", userId, workspaceId);
                    return new ServiceException(ErrorType.FORBIDDEN, MSG_WORKSPACE_NOT_LINKED_TO_USER, workspaceId, userId);
                });
    }

    private File validateFile(UUID fileId, UUID workspaceId) {
        log.debug("Validating file: fileId={}, workspaceId={}", fileId, workspaceId);
        var file = fileRepository.findById(fileId).orElseThrow(() -> {
            log.error("File not found: {}", fileId);
            return new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_NOT_FOUND, fileId);
        });

        if (!file.getWorkspace().getId().equals(workspaceId)) {
            log.error("File does not belong to workspace: fileId={}, workspaceId={}", fileId, workspaceId);
            throw new ServiceException(ErrorType.FORBIDDEN, MSG_FILE_NOT_IN_WORKSPACE, fileId, workspaceId);
        }
        return file;
    }

    private String validateAndGeneratePath(MultipartFile file, Workspace workspace) {
        log.debug("Validating and generating path for file: {}", file.getOriginalFilename());
        var originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            log.error("Invalid file name");
            throw new ServiceException(ErrorType.BAD_REQUEST, MSG_FILE_INVALID_NAME);
        }
        var fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension(originalFileName);
        var fileExtension = FileUtils.getFileExtension(originalFileName);
        return FileUtils.generateRelativePath(workspace.getName(), fileNameWithoutExtension, fileExtension);
    }

    private Optional<File> findExistingFile(Workspace workspace, String fileNameWithoutExtension, String fileExtension) {
        log.debug("Checking if file exists: workspace={}, fileName={}, extension={}",
                workspace.getId(), fileNameWithoutExtension, fileExtension);
        return fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, fileNameWithoutExtension, fileExtension);
    }

    private void uploadToS3(MultipartFile file, String bucketName, String path) {
        log.debug("Uploading file to S3: bucket={}, path={}", bucketName, path);
        s3Client.uploadFile(file, bucketName, path);
        log.info("File uploaded to S3: bucket={}, path={}", bucketName, path);
    }
}