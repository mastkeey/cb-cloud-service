package ru.mastkey.cloudservice.controller.file;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.cloudservice.util.FileUtils;
import ru.mastkey.model.ErrorResponse;
import ru.mastkey.model.PageFileResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void uploadFileSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);

        ClassPathResource resource = new ClassPathResource("files/testfile.txt");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.addAll(createAuthHeader(token));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity(
                "/api/v1/files/workspaces/" + savedWorkspace.getId(),
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var workspace = workspaceRepository.findByIdWithFiles(savedWorkspace.getId()).get();
        assertThat(workspace.getFiles().size()).isEqualTo(1);
    }

    @Test
    void uploadFilesWithSameFilenamesSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);

        ClassPathResource resource1 = new ClassPathResource("files/testfile.txt");
        ClassPathResource resource2 = new ClassPathResource("files/testfile.csv");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", resource1);
        body.add("files", resource2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.addAll(createAuthHeader(token));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity(
                "/api/v1/files/workspaces/" + savedWorkspace.getId(),
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
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = String.format("/api/v1/files/%s/workspaces/%s", file.getId(), savedWorkspace.getId());

        ResponseEntity<byte[]> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                byte[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    void deleteFileSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var file = createFileInWorkspace(savedWorkspace);
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        String url = String.format("/api/v1/files/%s/workspaces/%s", file.getId(), savedWorkspace.getId());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
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
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = String.format("/api/v1/files/workspaces/%s", savedWorkspace.getId());

        ResponseEntity<PageFileResponse> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
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
        String bucketName = workspace.getUsers().get(0).getBucketName();
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
    void deleteFileNotFoundTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var randomFileId = UUID.randomUUID();
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        String url = String.format("/api/v1/files/%s/workspaces/%s", randomFileId, savedWorkspace.getId());

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var error = response.getBody();
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.getMessage()).contains("File with id");
    }

    @Test
    void uploadFileWithoutTokenUnauthorizedTest() {
        var savedWorkspace = createWorkspaceWithUser();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

        ResponseEntity<Void> response = testRestTemplate.postForEntity(
                "/api/v1/files/workspaces/" + savedWorkspace.getId(),
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getFileWithoutTokenUnauthorizedTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var file = createFileInWorkspace(savedWorkspace);

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = String.format("/api/v1/files/%s/workspaces/%s", file.getId(), savedWorkspace.getId());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteFileWithoutTokenUnauthorizedTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var file = createFileInWorkspace(savedWorkspace);

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = String.format("/api/v1/files/%s/workspaces/%s", file.getId(), savedWorkspace.getId());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getFilesInfoWithoutTokenUnauthorizedTest() {
        var savedWorkspace = createWorkspaceWithUser();

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = String.format("/api/v1/files/workspaces/%s", savedWorkspace.getId());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}