package ru.mastkey.cloudservice.controller.workspace;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.model.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceControllerIntegrationTest extends IntegrationTestBase {
    private static final String BASE_URL = "/api/v1/workspaces";

    @Test
    void createWorkspaceSuccessTest() {
        var user = createUser();
        var token = createTokenForSavedUser(user);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        var request = new CreateWorkspaceRequest();
        request.setName("test");

        HttpEntity<CreateWorkspaceRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<WorkspaceResponse> response = testRestTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                requestEntity,
                WorkspaceResponse.class);

        var workspaceResponse = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var savedWorkspace = workspaceRepository.findById(workspaceResponse.getWorkspaceId());
        var savedUser = userRepository.findByUserIdWithWorkspaces(user.getId()).get();

        assertThat(savedWorkspace).isNotEmpty();
        assertThat(savedWorkspace.get().getName()).isEqualTo(workspaceResponse.getName());
        assertThat(savedUser.getWorkspaces().size()).isEqualTo(1);
    }

    @Test
    void getWorkspacesSuccessTest() {
        var savedWorkspace = createWorkspaceWithUser();
        var savedUser = savedWorkspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<PageWorkspaceResponse> response = testRestTemplate.exchange(
                BASE_URL,
                HttpMethod.GET,
                requestEntity,
                PageWorkspaceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var pageWorkspaces = response.getBody();
        assertThat(pageWorkspaces.getContent().size()).isEqualTo(1);
        assertThat(pageWorkspaces.getTotalPages()).isEqualTo(1);
        assertThat(pageWorkspaces.getTotalElements()).isEqualTo(1);
    }

    @Test
    void changeWorkspaceNameSuccessTest() {
        var workspace = createWorkspaceWithUser();
        var newName = "UpdatedWorkspaceName";
        var workspaceId = workspace.getId();
        var savedUser = workspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);
        workspace.setOwnerId(savedUser.getId());
        workspaceRepository.save(workspace);

        var changeRequest = new ChangeWorkspaceNameRequest(newName);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<ChangeWorkspaceNameRequest> requestEntity = new HttpEntity<>(changeRequest, headers);

        String urlWithParams = String.format("/api/v1/workspaces/%s",
                workspaceId);

        ResponseEntity<WorkspaceResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.PATCH,
                requestEntity,
                WorkspaceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var updatedWorkspace = workspaceRepository.findById(workspaceId);

        assertThat(updatedWorkspace).isNotEmpty();
        assertThat(updatedWorkspace.get().getName()).isEqualTo(newName);
    }

    @Test
    void changeWorkspaceNameNotFoundTest() {
        var workspace = createWorkspaceWithUser();
        var savedUser = workspace.getUsers().get(0);
        var token = createTokenForSavedUser(savedUser);
        workspace.setOwnerId(savedUser.getId());
        workspaceRepository.save(workspace);

        var randomWorkspaceId = UUID.randomUUID();
        var newName = "NonExistentWorkspace";

        var changeRequest = new ChangeWorkspaceNameRequest(newName);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<ChangeWorkspaceNameRequest> requestEntity = new HttpEntity<>(changeRequest, headers);

        String urlWithParams = String.format("/api/v1/workspaces/%s",
                randomWorkspaceId);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.PATCH,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).isEqualTo(
                String.format("Workspace with id %s not found", randomWorkspaceId)
        );
    }

    @Test
    void deleteWorkspaceSuccessTest() {
        var workspace = createWorkspaceWithUser();
        var user = workspace.getUsers().get(0);
        workspace.setOwnerId(user.getId());
        workspaceRepository.save(workspace);

        var token = createTokenForSavedUser(user);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        var workspaceId = workspace.getId();

        String urlWithParams = String.format("/api/v1/workspaces/%s", workspaceId);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deletedWorkspace = workspaceRepository.findById(workspaceId);
        assertThat(deletedWorkspace).isEmpty();
    }

    @Test
    void deleteWorkspaceNotFoundTest() {
        var workspace = createWorkspaceWithUser();
        var user = workspace.getUsers().get(0);
        var randomWorkspaceId = UUID.randomUUID();

        var token = createTokenForSavedUser(user);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String urlWithParams = String.format("/api/v1/workspaces/%s", randomWorkspaceId);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).isEqualTo(
                String.format("Workspace with ID %s is not linked to user with ID %s.", randomWorkspaceId, user.getId())
        );
    }

    @Test
    void getAllWorkspacesSuccessTest() {
        var workspace = createWorkspaceWithUser();
        var user = workspace.getUsers().get(0);
        var token = createTokenForSavedUser(user);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(createAuthHeader(token));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = "/api/v1/workspaces/all";

        ResponseEntity<List<WorkspaceResponse>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<WorkspaceResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var workspaces = response.getBody();
        assertThat(workspaces).isNotNull();
        assertThat(workspaces.size()).isEqualTo(1);
    }

    @Test
    void createWorkspaceWithoutTokenUnauthorizedTest() {
        var request = new CreateWorkspaceRequest();
        request.setName("UnauthorizedWorkspace");

        HttpEntity<CreateWorkspaceRequest> requestEntity = new HttpEntity<>(request);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getWorkspacesWithoutTokenUnauthorizedTest() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                BASE_URL,
                HttpMethod.GET,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changeWorkspaceNameWithoutTokenUnauthorizedTest() {
        var changeRequest = new ChangeWorkspaceNameRequest("UnauthorizedNameChange");

        HttpEntity<ChangeWorkspaceNameRequest> requestEntity = new HttpEntity<>(changeRequest);

        String urlWithParams = String.format("%s/%s", BASE_URL, UUID.randomUUID());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.PATCH,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteWorkspaceWithoutTokenUnauthorizedTest() {
        String urlWithParams = String.format("%s/%s", BASE_URL, UUID.randomUUID());

        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAllWorkspacesWithoutTokenUnauthorizedTest() {
        var url = "/api/v1/workspaces/all";

        HttpEntity<Void> requestEntity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<Void> response = testRestTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
