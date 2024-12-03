package ru.mastkey.cloudservice.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mastkey.cloudservice.controller.UserController;
import ru.mastkey.cloudservice.service.UserService;
import ru.mastkey.model.CreateUserRequest;
import ru.mastkey.model.CreateUserResponse;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_ShouldReturnUserResponse() throws Exception {
        var testName = "testuser";
        var request = new CreateUserRequest();
        request.setUsername(testName);

        var userResponse = new CreateUserResponse();
        userResponse.setId(UUID.randomUUID());
        userResponse.workspaceId(UUID.randomUUID());

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(userResponse)));
    }

    @Test
    void createUser_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        var request = new CreateUserRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

//    @Test
//    void addNewWorkspace_ShouldReturnOk() throws Exception {
//        var userId = UUID.randomUUID();
//        var workspaceId = UUID.randomUUID();
//
//        doNothing().when(userService).addNewWorkspace(userId, workspaceId);
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/{userId}/workspaces/{workspaceId}", userId, workspaceId)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//    }
}