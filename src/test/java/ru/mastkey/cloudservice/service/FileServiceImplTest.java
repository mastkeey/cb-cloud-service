package ru.mastkey.cloudservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.service.impl.FileServiceImpl;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.model.PageFileResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private MockedStatic<FileUtils> fileUtilsMockedStatic;

    @Mock
    private S3Client s3Client;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserWorkspaceRepository userWorkspaceRepository;

    @Mock
    private ConversionService conversionService;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private HttpContextService httpContextService;

    @InjectMocks
    private FileServiceImpl fileServiceImpl;

    private User user;
    private Workspace workspace;
    private UserWorkspace userWorkspace;

    @BeforeEach
    void setUp() {
        fileUtilsMockedStatic = mockStatic(FileUtils.class);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setBucketName("test_bucket");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setName("test_workspace");

        userWorkspace = new UserWorkspace();
        userWorkspace.setUser(user);
        userWorkspace.setWorkspace(workspace);
    }

    @Test
    void uploadFiles_ShouldUploadFilesSuccessfully() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());

        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.of(userWorkspace));
        when(fileRepository.findByWorkspaceAndFileNameAndFileExtension(any(), any(), any())).thenReturn(Optional.empty());
        when(multipartFile.getOriginalFilename()).thenReturn("testfile.txt");

        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("testfile.txt")).thenReturn("testfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("testfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(any(), eq("testfile"), eq("txt")))
                .thenReturn("generated/relative/path/to/testfile.txt");

        fileServiceImpl.uploadFiles(workspace.getId(), List.of(multipartFile));

        verify(fileRepository).save(any(File.class));
        verify(s3Client).uploadFile(eq(multipartFile), eq("test_bucket"), eq("generated/relative/path/to/testfile.txt"));
    }

    @Test
    void uploadFiles_ShouldThrowException_WhenUserNotLinkedToWorkspace() {
        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId())).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFiles(workspace.getId(), List.of(multipartFile)));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void deleteFile_ShouldDeleteFileSuccessfully() {
        var fileId = UUID.randomUUID();
        var file = new File();
        workspace.setOwner(user);
        file.setId(fileId);
        file.setPath("path/to/testfile.txt");
        file.setWorkspace(workspace);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.of(userWorkspace));

        fileServiceImpl.deleteFile(fileId, workspace.getId());

        verify(fileRepository).findById(fileId);
        verify(s3Client).deleteFile(eq(user.getBucketName()), eq(file.getPath()));
        verify(fileRepository).delete(file);
    }

    @Test
    void deleteFile_ShouldThrowException_WhenFileNotInWorkspace() {
        var fileId = UUID.randomUUID();
        var file = new File();
        workspace.setOwner(user);
        file.setId(fileId);
        file.setPath("path/to/testfile.txt");
        file.setWorkspace(new Workspace().setId(UUID.randomUUID()));

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.deleteFile(fileId, workspace.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(s3Client, never()).deleteFile(anyString(), anyString());
        verify(fileRepository, never()).delete(file);
    }

    @Test
    void downloadFile_ShouldReturnFileContent() {
        workspace.setOwner(user);
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setId(fileId);
        file.setPath("path/to/testfile.txt");
        file.setWorkspace(workspace);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.of(userWorkspace));
        when(s3Client.getFileStream(user.getBucketName(), "path/to/testfile.txt"))
                .thenReturn(new ByteArrayInputStream("file content".getBytes()));

        var result = fileServiceImpl.downloadFile(fileId, workspace.getId());

        assertThat(result).isNotNull();
        assertThat(result.file()).isEqualTo(file);
        assertThat(result.inputStream()).isNotNull();
        verify(fileRepository).findById(fileId);
        verify(s3Client).getFileStream(eq(user.getBucketName()), eq(file.getPath()));
    }

    @Test
    void downloadFile_ShouldThrowException_WhenUserNotLinkedToWorkspace() {
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setId(fileId);
        file.setPath("path/to/testfile.txt");
        file.setWorkspace(workspace);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.downloadFile(fileId, workspace.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(s3Client, never()).getFileStream(anyString(), anyString());
    }

    @Test
    void deleteFile_ShouldThrowException_WhenFileNotFound() {
        var fileId = UUID.randomUUID();

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.deleteFile(fileId, workspace.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("File with id " + fileId + " not found");
        verify(fileRepository).findById(fileId);
        verify(s3Client, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void deleteFile_ShouldThrowException_WhenUserNotLinkedToWorkspace() {
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setId(fileId);
        file.setWorkspace(workspace);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.deleteFile(fileId, workspace.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(fileRepository).findById(fileId);
        verify(s3Client, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void downloadFile_ShouldThrowException_WhenFileNotInWorkspace() {
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setId(fileId);
        file.setWorkspace(new Workspace().setId(UUID.randomUUID()));

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.downloadFile(fileId, workspace.getId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(fileRepository).findById(fileId);
        verify(s3Client, never()).getFileStream(anyString(), anyString());
    }

    @Test
    void getFilesInfo_ShouldThrowException_WhenUserNotLinkedToWorkspace() {
        var pageRequest = PageRequest.of(0, 10);

        when(httpContextService.getUserIdFromJwtToken()).thenReturn(user.getId());
        when(userWorkspaceRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()))
                .thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.getFilesInfo(workspace.getId(), pageRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.FORBIDDEN.getCode());
        verify(fileRepository, never()).findAll(any(Specification.class), eq(pageRequest));
        verify(conversionService, never()).convert(any(), eq(PageFileResponse.class));
    }

    @Test
    void uploadFile_ShouldThrowException_WhenS3UploadFails() {
        when(multipartFile.getOriginalFilename()).thenReturn("testfile.txt");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("testfile.txt")).thenReturn("testfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("testfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace.getName(), "testfile", "txt"))
                .thenReturn("generated/relative/path/to/testfile.txt");

        doThrow(new RuntimeException("S3 upload failed")).when(s3Client)
                .uploadFile(eq(multipartFile), eq(user.getBucketName()), eq("generated/relative/path/to/testfile.txt"));

        var exception = assertThrows(RuntimeException.class,
                () -> fileServiceImpl.uploadFile(multipartFile, workspace, user));

        assertThat(exception.getMessage()).isEqualTo("S3 upload failed");
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void uploadFile_ShouldReplaceExistingFile() {
        var existingFile = new File();
        existingFile.setPath("path/to/existingfile.txt");
        workspace.setOwner(user);

        when(multipartFile.getOriginalFilename()).thenReturn("existingfile.txt");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("existingfile.txt")).thenReturn("existingfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("existingfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace.getName(), "existingfile", "txt"))
                .thenReturn("path/to/existingfile.txt");
        when(fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, "existingfile", "txt"))
                .thenReturn(Optional.of(existingFile));

        fileServiceImpl.uploadFile(multipartFile, workspace, user);

        verify(s3Client).uploadFile(eq(multipartFile), eq(user.getBucketName()), eq(existingFile.getPath()));
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void uploadFile_ShouldThrowException_WhenOriginalFileNameIsNull() {
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFile(multipartFile, workspace, user));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("Invalid file name");
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenOriginalFileNameIsBlank() {
        when(multipartFile.getOriginalFilename()).thenReturn("   ");

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFile(multipartFile, workspace, user));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("Invalid file name");
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @AfterEach
    void tearDown() {
        fileUtilsMockedStatic.close();
    }
}