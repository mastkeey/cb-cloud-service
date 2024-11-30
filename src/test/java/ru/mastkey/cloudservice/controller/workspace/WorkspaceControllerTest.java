package ru.mastkey.cloudservice.controller.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.mastkey.cloudservice.configuration.properties.Properties;
import ru.mastkey.cloudservice.controller.WorkspaceController;
import ru.mastkey.cloudservice.service.WorkspaceService;
import ru.mastkey.model.CreateWorkspaceRequest;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private Properties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWorkspace_ShouldReturnWorkspaceResponse() throws Exception {
        var testWorkspaceName = "test-workspace";

        var request = new CreateWorkspaceRequest();
        request.setTelegramUserId(12345L);
        request.setName(testWorkspaceName);

        var workspaceResponse = new WorkspaceResponse();
        workspaceResponse.setName(testWorkspaceName);

        when(workspaceService.createWorkspace(any(CreateWorkspaceRequest.class))).thenReturn(workspaceResponse);
        when(properties.getPageSize()).thenReturn(10);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(testWorkspaceName));
    }

    @Test
    void createWorkspace_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        var request = new CreateWorkspaceRequest();
        request.setName("test_workspace");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeWorkspaceName_ShouldReturnUpdatedWorkspaceResponse() throws Exception {
        var workspaceId = UUID.randomUUID();
        var newWorkspaceName = "updated-workspace";

        var updatedWorkspaceResponse = new WorkspaceResponse();
        updatedWorkspaceResponse.setName(newWorkspaceName);

        when(workspaceService.changeWorkspaceName(eq(workspaceId), eq(newWorkspaceName)))
                .thenReturn(updatedWorkspaceResponse);

        mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", workspaceId)
                        .param("newWorkspaceName", newWorkspaceName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(newWorkspaceName));
    }


    @Test
    void changeWorkspaceName_ShouldReturnBadRequest() throws Exception {
        var workspaceId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWorkspaces_ShouldReturnPagedWorkspaceList() throws Exception {
        var telegramUserId = 12345L;

        var workspace1 = new WorkspaceResponse();
        workspace1.setName("Workspace 1");

        var workspace2 = new WorkspaceResponse();
        workspace2.setName("Workspace 2");

        var workspaceList = List.of(workspace1, workspace2);
        Page<WorkspaceResponse> pagedWorkspaces = new PageImpl<>(workspaceList, PageRequest.of(0, 2), 2);

        when(workspaceService.getWorkspaces(eq(telegramUserId), any(PageRequest.class)))
                .thenReturn(pagedWorkspaces);

        mockMvc.perform(get("/api/v1/workspaces/users/{telegramUserId}", telegramUserId)
                        .param("pageNumber", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Workspace 1"))
                .andExpect(jsonPath("$[1].name").value("Workspace 2"))
                .andExpect(header().string("Total-Pages", "1"));
    }


    @Test
    void deleteWorkspace_ShouldReturnOk() throws Exception {
        var workspaceId = UUID.randomUUID();

        doNothing().when(workspaceService).deleteWorkspace(workspaceId);

        mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isOk());
    }
}