package ru.mastkey.cloudservice.controller.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mastkey.cloudservice.client.model.FileContent;
import ru.mastkey.cloudservice.configuration.properties.Properties;
import ru.mastkey.cloudservice.controller.FileController;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.service.FileService;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Properties properties;

    @Test
    void uploadFiles_ShouldReturnOk() throws Exception {
        var userId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("files", "testfile.txt",
                "text/plain", "test content".getBytes());

        doNothing().when(fileService).uploadFiles(eq(userId), eq(workspaceId), anyList());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files/users/{userId}/workspaces/{workspaceId}", userId, workspaceId)
                        .file(file))
                .andExpect(status().isOk());

        verify(fileService).uploadFiles(eq(userId), eq(workspaceId), anyList());
    }

    @Test
    void getFilesInfo_ShouldReturnFilesList() throws Exception {
        var userId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        var fileResponse = new FileResponse();
        fileResponse.setFileName("testfile.txt");

        var fileList = List.of(fileResponse);
        PageFileResponse pagedFiles = new PageFileResponse();
        pagedFiles.setContent(fileList);
        pagedFiles.setTotalPages(1);
        pagedFiles.setTotalElements(1);

        var pageRequest = PageRequest.of(0, 10);
        when(properties.getPageSize()).thenReturn(10);
        when(fileService.getFilesInfo(eq(userId), eq(workspaceId), eq(pageRequest))).thenReturn(pagedFiles);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/users/{userId}/workspaces/{workspaceId}", userId, workspaceId)
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].fileName").value("testfile.txt"))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(fileService).getFilesInfo(eq(userId), eq(workspaceId), eq(pageRequest));
    }

    @Test
    void deleteFile_ShouldReturnOk() throws Exception {
        var fileId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        doNothing().when(fileService).deleteFile(fileId, workspaceId, userId);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/files/{fileId}/users/{userId}/workspaces/{workspaceId}",
                        fileId, userId, workspaceId))
                .andExpect(status().isOk());

        verify(fileService).deleteFile(fileId, workspaceId, userId);
    }

    @Test
    void getFile_ShouldReturnFileResource() throws Exception {
        var fileId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var file = new File();
        file.setFileName("testfile");
        file.setFileExtension("txt");

        var fileContent = new FileContent(new ByteArrayInputStream("file content".getBytes()), file);

        when(fileService.downloadFile(fileId, workspaceId, userId)).thenReturn(fileContent);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/{fileId}/users/{userId}/workspaces/{workspaceId}",
                        fileId, userId, workspaceId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=testfile.txt"))
                .andExpect(content().contentType("application/octet-stream"));

        verify(fileService).downloadFile(fileId, workspaceId, userId);
    }
}