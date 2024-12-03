package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.CreateUserRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserRequestToUserMapperTest {

    private CreateUserRequestToUserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CreateUserRequestToUserMapper.class);
    }

    @Test
    void shouldMapTelegramUserIdToBucketName() {
        CreateUserRequest request = new CreateUserRequest()
                .username("username");

        User user = mapper.convert(request);

        assertThat(user).isNotNull();
    }

}