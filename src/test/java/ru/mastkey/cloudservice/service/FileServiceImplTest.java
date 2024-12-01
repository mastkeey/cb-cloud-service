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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.exception.ErrorType;
import ru.mastkey.cloudservice.exception.ServiceException;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.service.impl.FileServiceImpl;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.model.FileResponse;
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
    private WorkspaceRepository workspaceRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversionService conversionService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileServiceImpl fileServiceImpl;

    private User user;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        fileUtilsMockedStatic = mockStatic(FileUtils.class);

        var testWorkspaceName = "test-workspace";

        user = new User();
        user.setTelegramUserId(12345L);
        user.setBucketName("test_bucket");

        workspace = new Workspace();
        workspace.setName(testWorkspaceName);
        workspace.setUser(user);
        user.setWorkspaces(List.of(workspace));
        user.setCurrentWorkspace(workspace);
    }

    @Test
    void uploadFiles_ShouldUploadFilesSuccessfully() {
        when(userRepository.findByTelegramUserIdWithWorkspaces(user.getTelegramUserId())).thenReturn(Optional.of(user));
        when(fileRepository.findByWorkspaceAndFileNameAndFileExtension(any(), any(), any())).thenReturn(Optional.empty());
        when(multipartFile.getOriginalFilename()).thenReturn("testfile.txt");

        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("testfile.txt")).thenReturn("testfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("testfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(any(), eq("testfile"), eq("txt")))
                .thenReturn("generated/relative/path/to/testfile.txt");

        fileServiceImpl.uploadFiles(List.of(multipartFile), user.getTelegramUserId());

        verify(fileRepository).save(any(File.class));
        verify(s3Client).uploadFile(eq(multipartFile), eq("test_bucket"), eq("generated/relative/path/to/testfile.txt"));
    }

    @Test
    void uploadFiles_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByTelegramUserIdWithWorkspaces(user.getTelegramUserId())).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFiles(List.of(multipartFile), user.getTelegramUserId()));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileNameIsBlank() {
        when(multipartFile.getOriginalFilename()).thenReturn("");

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFile(multipartFile, workspace));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("Invalid file name");
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileNameIsNull() {
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.uploadFile(multipartFile, workspace));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("Invalid file name");
        verify(fileRepository, never()).save(any(File.class));
        verify(s3Client, never()).uploadFile(any(), anyString(), anyString());
    }

    @Test
    void uploadFile_ShouldUploadFile_WhenFileExists() {
        when(multipartFile.getOriginalFilename()).thenReturn("existingfile.txt");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("existingfile.txt")).thenReturn("existingfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("existingfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace, "existingfile", "txt"))
                .thenReturn("generated/relative/path/to/existingfile.txt");
        when(fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, "existingfile", "txt"))
                .thenReturn(Optional.of(new File()));

        fileServiceImpl.uploadFile(multipartFile, workspace);

        verify(s3Client).uploadFile(eq(multipartFile), anyString(), anyString());
        verify(fileRepository, never()).save(any(File.class));
    }

    @Test
    void uploadFile_ShouldSaveAndUploadFile_WhenFileDoesNotExist() {
        when(multipartFile.getOriginalFilename()).thenReturn("newfile.txt");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileNameWithoutExtension("newfile.txt")).thenReturn("newfile");
        fileUtilsMockedStatic.when(() -> FileUtils.getFileExtension("newfile.txt")).thenReturn("txt");
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace, "newfile", "txt"))
                .thenReturn("generated/relative/path/to/newfile.txt");
        when(fileRepository.findByWorkspaceAndFileNameAndFileExtension(workspace, "newfile", "txt"))
                .thenReturn(Optional.empty());

        fileServiceImpl.uploadFile(multipartFile, workspace);

        verify(fileRepository).save(any(File.class));
        verify(s3Client).uploadFile(eq(multipartFile), anyString(), anyString());
    }

    @Test
    void getFilesInfo_ShouldReturnFileResponsesSuccessfully() {
        var telegramUserId = 12345L;
        var pageRequest = PageRequest.of(0, 10);
        var file = new File();
        file.setId(UUID.randomUUID());
        file.setFileName("testfile");
        file.setFileExtension("txt");

        var files = List.of(file);
        var fileResponse = new FileResponse();
        var pagedFiles = new PageImpl<>(files, pageRequest, files.size());
        var workspaceId = UUID.randomUUID();
        workspace.setId(workspaceId);

        var pageResponse = new PageFileResponse();
        pageResponse.setContent(List.of(fileResponse));
        pageResponse.setTotalPages(1);
        pageResponse.setTotalElements(1);

        when(userRepository.findByTelegramUserIdWithWorkspaces(eq(telegramUserId))).thenReturn(Optional.of(user));
        when(fileRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(pagedFiles);
        when(conversionService.convert(file, FileResponse.class)).thenReturn(fileResponse);
        when(conversionService.convert(any(Page.class), eq(PageFileResponse.class))).thenReturn(pageResponse);

        var result = fileServiceImpl.getFilesInfo(telegramUserId, pageRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(fileResponse);

        verify(userRepository).findByTelegramUserIdWithWorkspaces(eq(telegramUserId));
        verify(fileRepository).findAll(any(Specification.class), eq(pageRequest));
        verify(conversionService).convert(file, FileResponse.class);
    }

    @Test
    void getFilesInfo_ShouldThrowException_WhenUserNotFound() {
        var telegramUserId = 12345L;
        var pageRequest = PageRequest.of(0, 10);

        when(userRepository.findByTelegramUserIdWithWorkspaces(telegramUserId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class,
                () -> fileServiceImpl.getFilesInfo(telegramUserId, pageRequest));

        assertThat(exception.getCode()).isEqualTo(ErrorType.NOT_FOUND.getCode());
        assertThat(exception.getMessage()).isEqualTo(String.format("User with id %s not found", telegramUserId));
        verify(userRepository).findByTelegramUserIdWithWorkspaces(telegramUserId);
    }

    @Test
    void deleteFile_ShouldDeleteFileSuccessfully() {
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setFileName("testfile");
        file.setFileExtension("txt");
        file.setWorkspace(workspace);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace, "testfile", "txt"))
                .thenReturn("path/to/testfile.txt");

        fileServiceImpl.deleteFile(fileId);

        verify(fileRepository).findById(fileId);
        verify(s3Client).deleteFile(eq(user.getBucketName()), eq("path/to/testfile.txt"));
    }

    @Test
    void deleteFile_ShouldThrowException_WhenFileNotFound() {
        var fileId = UUID.randomUUID();

        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> fileServiceImpl.deleteFile(fileId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("File with id " + fileId + " not found");
        verify(s3Client, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void downloadFile_ShouldReturnFileContent() {
        var fileId = UUID.randomUUID();
        var file = new File();
        file.setFileName("testfile");
        file.setFileExtension("txt");
        file.setWorkspace(workspace);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        fileUtilsMockedStatic.when(() -> FileUtils.generateRelativePath(workspace, "testfile", "txt"))
                .thenReturn("path/to/testfile.txt");
        when(s3Client.getFileStream(user.getBucketName(), "path/to/testfile.txt"))
                .thenReturn(new ByteArrayInputStream("file content" .getBytes()));

        var result = fileServiceImpl.downloadFile(fileId);

        assertThat(result).isNotNull();
        assertThat(result.file()).isEqualTo(file);
        assertThat(result.inputStream()).isNotNull();
        verify(fileRepository).findById(fileId);
        verify(s3Client).getFileStream(eq(user.getBucketName()), eq("path/to/testfile.txt"));
    }

    @Test
    void downloadFile_ShouldThrowException_WhenFileNotFound() {
        var fileId = UUID.randomUUID();

        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        var exception = assertThrows(ServiceException.class, () -> fileServiceImpl.downloadFile(fileId));

        assertThat(exception.getCode()).isEqualTo(ErrorType.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).isEqualTo("File with id " + fileId + " not found");
        verify(s3Client, never()).getFileStream(anyString(), anyString());
    }


    @AfterEach
    void tearDown() {
        fileUtilsMockedStatic.close();
    }
}