package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.CreateUserResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserToCreateUserResponseMapperTest {

    private UserToCreateUserResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserToCreateUserResponseMapper.class);
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        var testWorkspace = new Workspace();
        testWorkspace.setId(id);
        User user = User.builder()
                .id(id)
                .build();
        user.setWorkspaces(List.of(testWorkspace));

        CreateUserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        UUID id = UUID.randomUUID();
        var testWorkspace = new Workspace();
        testWorkspace.setId(id);
        User user = new User();
        user.setWorkspaces(List.of(testWorkspace));

        CreateUserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
    }
}