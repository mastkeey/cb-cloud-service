package ru.mastkey.cloudservice.controller.workspace;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.ErrorResponse;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceControllerIntegrationTest extends IntegrationTestBase {
    private static final String BASE_URL = "/api/v1/workspaces";

    @Test
    void createWorkspaceSuccessTest() {
        var user = createUser();
        var request = new CreateWorkspaceRequest();
        request.setName("test");
        request.setTelegramUserId(user.getTelegramUserId());

        ResponseEntity<WorkspaceResponse> response =  testRestTemplate
                .postForEntity(BASE_URL, request, WorkspaceResponse.class);

        var workspaceResponse = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var savedWorkspace = workspaceRepository.findById(workspaceResponse.getWorkspaceId());
        var savedUser = userRepository.findByTelegramUserIdWithWorkspaces(workspaceResponse.getTelegramUserId()).get();

        assertThat(savedWorkspace).isNotEmpty();
        assertThat(savedWorkspace.get().getName()).isEqualTo(workspaceResponse.getName());
        assertThat(savedWorkspace.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(savedUser.getWorkspaces().size()).isEqualTo(1);
    }

    @Test
    void createWorkspaceNotFoundTest() {
        var testId = 123L;
        var request = new CreateWorkspaceRequest();
        request.setName("test");
        request.setTelegramUserId(testId);

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).isEqualTo("User with id %s not found".formatted(testId));
    }

    @Test
    void getWorkspacesUserNotFoundTest() {
        var testUserId = 123123L;

        String urlWithParams = String.format("/api/v1/workspaces/users/%s",
                testUserId);

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .getForEntity(urlWithParams, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(error.getMessage()).isEqualTo(String.format("User with id %s not found", testUserId));
    }

    @Test
    void getWorkspacesSuccessTest() {
        var testUserId = createWorkspaceWithUser().getUser().getTelegramUserId();

        String urlWithParams = String.format("/api/v1/workspaces/users/%s",
                testUserId);

        ResponseEntity<List<WorkspaceResponse>> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<WorkspaceResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var workspaces = response.getBody();
        assertThat(workspaces.size()).isEqualTo(1);
    }

    @Test
    void changeWorkspaceNameSuccessTest() {
        var workspace = createWorkspaceWithUser();
        var workspaceId = workspace.getId();
        var newName = "UpdatedWorkspaceName";

        String urlWithParams = String.format("/api/v1/workspaces/%s?newWorkspaceName=%s",
                workspaceId, newName);

        ResponseEntity<WorkspaceResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.PATCH,
                null,
                WorkspaceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var updatedWorkspace = workspaceRepository.findById(workspaceId);

        assertThat(updatedWorkspace).isNotEmpty();
        assertThat(updatedWorkspace.get().getName()).isEqualTo(newName);
    }

    @Test
    void changeWorkspaceNameNotFoundTest() {
        var randomWorkspaceId = UUID.randomUUID();
        var newName = "NonExistentWorkspace";

        String urlWithParams = String.format("/api/v1/workspaces/%s?newWorkspaceName=%s",
                randomWorkspaceId, newName);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.PATCH,
                null,
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
        var user = workspace.getUser();
        var newWorkspace = new Workspace();
        newWorkspace.setName("testtest");
        newWorkspace.setUser(user);
        workspaceRepository.save(newWorkspace);
        user.getWorkspaces().add(newWorkspace);

        var workspaceId = newWorkspace.getId();

        String urlWithParams = String.format("/api/v1/workspaces/%s", workspaceId);

        ResponseEntity<Void> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deletedWorkspace = workspaceRepository.findById(workspaceId);
        assertThat(deletedWorkspace).isEmpty();
    }

    @Test
    void deleteWorkspaceNotFoundTest() {
        var randomWorkspaceId = UUID.randomUUID();

        String urlWithParams = String.format("/api/v1/workspaces/%s", randomWorkspaceId);

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                null,
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
    void deleteCurrentWorkspaceConflictTest() {
        var workspace = createWorkspaceWithUser();
        var user = workspace.getUser();
        user.setCurrentWorkspace(workspace);
        userRepository.save(user);

        String urlWithParams = String.format("/api/v1/workspaces/%s", workspace.getId());

        ResponseEntity<ErrorResponse> response = testRestTemplate.exchange(
                urlWithParams,
                HttpMethod.DELETE,
                null,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var error = response.getBody();

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(error.getMessage()).isEqualTo("You can't delete current workspace");
    }
}
