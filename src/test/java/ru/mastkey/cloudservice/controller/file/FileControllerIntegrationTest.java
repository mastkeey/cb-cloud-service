package ru.mastkey.cloudservice.controller.file;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.model.ErrorResponse;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void uploadFileSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();

        ClassPathResource resource1 = new ClassPathResource("files/testfile.txt");
        ClassPathResource bigResource = new ClassPathResource("files/large_file.txt");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource1);
        body.add("files", bigResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity(
                "/api/v1/files/users/" + savedWorkspace.getUser().getTelegramUserId(),
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var workspace = workspaceRepository.findByIdWithFiles(savedWorkspace.getId()).get();
        assertThat(workspace.getFiles().size()).isEqualTo(2);
    }

    @Test
    void uploadFilesWithSameFilenamesSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();

        ClassPathResource resource1 = new ClassPathResource("files/testfile.txt");
        ClassPathResource resource2 = new ClassPathResource("files/testfile.csv");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource1);
        body.add("files", resource2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity(
                "/api/v1/files/users/" + savedWorkspace.getUser().getTelegramUserId(),
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var workspace = workspaceRepository.findByIdWithFiles(savedWorkspace.getId()).get();
        assertThat(workspace.getFiles().size()).isEqualTo(2);
    }

    @Test
    void getFileSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var file = createFileInWorkspace(savedWorkspace);

        String url = String.format("/api/v1/files/%s", file.getId());

        ResponseEntity<byte[]> response = testRestTemplate.getForEntity(url, byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }


    @Test
    void deleteFileSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var file = createFileInWorkspace(savedWorkspace);

        String url = String.format("/api/v1/files/%s", file.getId());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        var deletedFile = fileRepository.findById(file.getId());
        assertThat(deletedFile).isEmpty();
    }

    @Test
    void getFilesInfoSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        createFileInWorkspace(savedWorkspace);
        createFileInWorkspace(savedWorkspace);

        String url = String.format("/api/v1/files/users/%s?pageNumber=0&pageSize=10",
                savedWorkspace.getUser().getTelegramUserId());

        ResponseEntity<PageFileResponse> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                PageFileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var files = response.getBody().getContent();
        assertThat(files).isNotNull();
        assertThat(files.size()).isEqualTo(2);
    }

    private File createFileInWorkspace(Workspace workspace) {
        var file = new File();
        file.setWorkspace(workspace);
        file.setFileName("testfile");
        file.setFileExtension(".txt");
        String bucketName = workspace.getUser().getBucketName();
        String s3Path = FileUtils.generateRelativePath(workspace.getName(), file.getFileName(), file.getFileExtension());
        file.setPath(s3Path);
        byte[] fileContent = "Test file content".getBytes();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "testfile.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test file content".getBytes()
        );
        s3Client.uploadFile(mockFile, bucketName, s3Path);
        return fileRepository.save(file);
    }

    @Test
    void uploadFileToNonExistingUserTest() {
        var nonExistingUserId = 14212L;

        ClassPathResource resource = new ClassPathResource("files/testfile.txt");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<ErrorResponse> response = testRestTemplate.postForEntity(
                "/api/v1/files/users/" + nonExistingUserId,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).contains("User with id");
    }

    @Test
    void deleteFileNotFoundTest() {
        var randomFileId = UUID.randomUUID();

        String url = String.format("/api/v1/files/%s", randomFileId);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.getMessage()).contains("File with id");
    }

    @Test
    void getFilesInfoUserNotFoundTest() {
        var nonExistingUserId = 12345L;

        String url = String.format("/api/v1/files/users/%s?pageNumber=0&pageSize=10", nonExistingUserId);

        ResponseEntity<ErrorResponse> response = testRestTemplate.getForEntity(url, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).contains("User with id");
    }

}