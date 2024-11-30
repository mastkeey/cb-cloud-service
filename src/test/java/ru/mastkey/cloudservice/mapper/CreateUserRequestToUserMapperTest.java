package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.CreateUserRequest;

import java.util.UUID;

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
                .telegramUserId(123456L);

        User user = mapper.convert(request);

        assertThat(user).isNotNull();
        assertThat(user.getBucketName()).isEqualTo("123456");
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        CreateUserRequest request = new CreateUserRequest()
                .username("john_doe")
                .telegramUserId(123456L)
                .chatId(7891011L);

        User user = mapper.convert(request);

        assertThat(user).isNotNull();
        assertThat(user.getTelegramUserId()).isEqualTo(123456L);
        assertThat(user.getChatId()).isEqualTo(7891011L);
        assertThat(user.getBucketName()).isEqualTo("123456");
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        CreateUserRequest request = new CreateUserRequest();

        User user = mapper.convert(request);

        assertThat(user).isNotNull();
        assertThat(user.getTelegramUserId()).isNull();
        assertThat(user.getChatId()).isNull();
    }
}