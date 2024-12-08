package ru.mastkey.cloudservice.controller.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.model.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE_URL = "/api/v1/users";
    private static final String WORKSPACE_URL = "/api/v1/workspaces/";

    @Test
    void createUserSuccessTest() {
        var request = createCreateUserRequest();

        ResponseEntity<CreateUserResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, CreateUserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var user = userRepository.findByUserIdWithWorkspaces(response.getBody().getId()).get();
        var body = response.getBody();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(body.getId()).isNotNull();
    }

    @Test
    void createUserBadRequestTest() {
        var request = createCreateUserRequest();
        request.setUsername(null);

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createUserConflictTest() {
        var user = new User();
        user.setUsername("mastkey");
        userRepository.save(user);

        var request = createCreateUserRequest();

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void authUserSuccessTest() {
        var user = new User();
        user.setUsername("testUser");
        user.setPassword(passwordEncoder.encode("testPassword"));
        userRepository.save(user);

        var authRequest = new AuthUserRequest();
        authRequest.setUsername("testUser");
        authRequest.setPassword("testPassword");

        ResponseEntity<TokenResponse> response = testRestTemplate
                .postForEntity("/api/v1/auth", authRequest, TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    @Test
    void authUserInvalidCredentialsTest() {
        var user = new User();
        user.setUsername("testUser");
        user.setPassword(passwordEncoder.encode("testPassword"));
        userRepository.save(user);

        var authRequest = new AuthUserRequest();
        authRequest.setUsername("testUser");
        authRequest.setPassword("wrongPassword");

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity("/api/v1/auth", authRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Incorrect password");
    }

    @Test
    void authUserNotFoundTest() {
        var authRequest = new AuthUserRequest();
        authRequest.setUsername("nonExistentUser");
        authRequest.setPassword("password");

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity("/api/v1/auth", authRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("User with username nonExistentUser not found");
    }

    @Test
    void addNewWorkspaceSuccessTest() {
        var user = createUser();
        var token = createTokenForSavedUser(user);

        var workspace = createWorkspaceWithUser();

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                WORKSPACE_URL + workspace.getId(),
                HttpMethod.POST,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var updatedUser = userRepository.findByUserIdWithWorkspaces(user.getId()).get();
        assertThat(updatedUser.getWorkspaces()).hasSize(1);
        assertThat(updatedUser.getWorkspaces().get(0).getName()).isEqualTo(workspace.getName());
    }

    @Test
    void addNewWorkspaceNotFoundTest() {
        var user = createUser();
        var token = createTokenForSavedUser(user);

        UUID nonExistentWorkspaceId = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                WORKSPACE_URL + nonExistentWorkspaceId,
                HttpMethod.POST,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(
                String.format("Workspace with id %s not found", nonExistentWorkspaceId)
        );
    }

    @Test
    void addNewWorkspaceConflictTest() {
        var user = createUser();
        var token = createTokenForSavedUser(user);

        var workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspaceRepository.save(workspace);

        userWorkspaceRepository.save(
                UserWorkspace.builder()
                        .workspace(workspace)
                        .user(user)
                        .build()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                WORKSPACE_URL + workspace.getId(),
                HttpMethod.POST,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo(
                String.format("Workspace %s already exists", workspace.getId())
        );
    }

    @Test
    void addNewWorkspaceUnauthorizedTest() {
        var workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspaceRepository.save(workspace);

        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                WORKSPACE_URL + workspace.getId(),
                HttpMethod.POST,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    public CreateUserRequest createCreateUserRequest() {
        var request = new CreateUserRequest();
        request.setUsername("mastkey");
        request.setPassword("mastkey");
        return request;
    }
}
