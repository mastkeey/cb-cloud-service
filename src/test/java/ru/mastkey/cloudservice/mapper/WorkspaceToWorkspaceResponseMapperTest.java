package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.WorkspaceResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceToWorkspaceResponseMapperTest {

    private WorkspaceToWorkspaceResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(WorkspaceToWorkspaceResponseMapper.class);
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        UUID workspaceId = UUID.randomUUID();
        User user = User.builder()
                .telegramUserId(123456L)
                .build();
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .user(user)
                .build();

        WorkspaceResponse response = mapper.convert(workspace);

        assertThat(response).isNotNull();
        assertThat(response.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(response.getName()).isEqualTo("Test Workspace");
        assertThat(response.getTelegramUserId()).isEqualTo(123456L);
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        Workspace workspace = Workspace.builder()
                .id(null)
                .name(null)
                .user(null)
                .build();

        WorkspaceResponse response = mapper.convert(workspace);

        assertThat(response).isNotNull();
        assertThat(response.getWorkspaceId()).isNull();
        assertThat(response.getName()).isNull();
        assertThat(response.getTelegramUserId()).isNull();
    }

    @Test
    void shouldMapNameOnly() {
        Workspace workspace = Workspace.builder()
                .name("Partial Workspace")
                .build();

        WorkspaceResponse response = mapper.convert(workspace);

        assertThat(response).isNotNull();
        assertThat(response.getWorkspaceId()).isNull();
        assertThat(response.getName()).isEqualTo("Partial Workspace");
        assertThat(response.getTelegramUserId()).isNull();
    }

    @Test
    void shouldHandleWorkspaceWithoutUser() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("No User Workspace")
                .user(null)
                .build();

        WorkspaceResponse response = mapper.convert(workspace);

        assertThat(response).isNotNull();
        assertThat(response.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(response.getName()).isEqualTo("No User Workspace");
        assertThat(response.getTelegramUserId()).isNull();
    }

    @Test
    void shouldHandleUserWithoutTelegramUserId() {
        UUID workspaceId = UUID.randomUUID();
        User user = User.builder()
                .telegramUserId(null)
                .build();
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .user(user)
                .build();

        WorkspaceResponse response = mapper.convert(workspace);

        assertThat(response).isNotNull();
        assertThat(response.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(response.getName()).isEqualTo("Test Workspace");
        assertThat(response.getTelegramUserId()).isNull();
    }
}