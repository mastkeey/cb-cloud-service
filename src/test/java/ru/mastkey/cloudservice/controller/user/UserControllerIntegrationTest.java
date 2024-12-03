package ru.mastkey.cloudservice.controller.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.support.IntegrationTestBase;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;
import ru.mastkey.model.ErrorResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIntegrationTest extends IntegrationTestBase {

    private static final String BASE_URL = "/api/v1/users";

    @Test
    void createUserSuccessTest() {
        var request = createCreateUserRequest();

        ResponseEntity<CreateUserResponse> response = testRestTemplate
                .postForEntity(BASE_URL, request, CreateUserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var user = userRepository.findByUserIdWithWorkspaces(response.getBody().getId()).get();
        var body = response.getBody();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getWorkspaces().get(0).getName()).isEqualTo(request.getUsername());
        assertThat(body.getId()).isNotNull();
    }

    public CreateUserRequest createCreateUserRequest() {
        var request = new CreateUserRequest();
        request.setUsername("mastkey");
        return request;
    }
}
