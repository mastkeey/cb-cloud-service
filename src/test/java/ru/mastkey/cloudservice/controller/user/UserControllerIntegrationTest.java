package ru.mastkey.cloudservice.controller.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.ErrorResponse;
import ru.mastkey.model.UserResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE_URL = "/api/v1/users";

    @Test
    void createUserSuccessTest() {
        var request = createCreateUserRequest();

        ResponseEntity<UserResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var user = userRepository.findByTelegramUserIdWithWorkspaces(request.getTelegramUserId()).get();
        var body = response.getBody();

        assertThat(user.getTelegramUserId()).isEqualTo(request.getTelegramUserId());
        assertThat(user.getChatId()).isEqualTo(request.getChatId());
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getWorkspaces().get(0).getName()).isEqualTo(request.getUsername());
        assertThat(body.getChatId()).isEqualTo(request.getChatId());
        assertThat(body.getTelegramUserId()).isEqualTo(request.getTelegramUserId());
        assertThat(body.getId()).isNotNull();
    }

    @Test
    void createUserConflictTest() {
        var request = createCreateUserRequest();
        var user = User.builder()
                .telegramUserId(request.getTelegramUserId())
                .chatId(request.getChatId())
                .build();

        userRepository.save(user);

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains(String.format("User with id %s already exist", request.getTelegramUserId()));
    }

    @Test
    void changeCurrentWorkspaceUserNotFountTest() {
        var testId = 123123L;
        var testWorkspaceId = UUID.randomUUID().toString();

        String urlWithParams = String.format("/api/v1/users/%s/changeCurrentWorkspace?newWorkspaceId=%s",
                testId,
                testWorkspaceId);

        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(urlWithParams, null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getMessage()).isEqualTo(String.format("User with id %s not found", testId));
    }

    @Test
    void changeCurrentWorkspaceWorkspaceNotFountTest() {
        var testId = createUser().getTelegramUserId();
        var testWorkspaceId = UUID.randomUUID().toString();

        String urlWithParams = String.format("/api/v1/users/%s/changeCurrentWorkspace?newWorkspaceId=%s",
                testId,
                testWorkspaceId);


        ResponseEntity<ErrorResponse> response = testRestTemplate
                .postForEntity(urlWithParams, null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var error = response.getBody();

        assertThat(error.getMessage()).isEqualTo(String.format("User with id %s does not have a Workspace with id %s", testId, testWorkspaceId));
    }


    public CreateUserRequest createCreateUserRequest() {
        var request = new CreateUserRequest();
        request.setTelegramUserId(123L);
        request.setChatId(321L);
        request.setUsername("mastkey");
        return request;
    }
}
