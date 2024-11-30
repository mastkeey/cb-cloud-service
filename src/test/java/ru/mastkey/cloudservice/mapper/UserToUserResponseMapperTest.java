package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.UserResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserToUserResponseMapperTest {

    private UserToUserResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserToUserResponseMapper.class);
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .telegramUserId(123456L)
                .chatId(7891011L)
                .build();

        UserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getTelegramUserId()).isEqualTo(123456L);
        assertThat(response.getChatId()).isEqualTo(7891011L);
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        User user = new User();

        UserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getTelegramUserId()).isNull();
        assertThat(response.getChatId()).isNull();
    }

    @Test
    void shouldMapIdOnly() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .build();

        UserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getTelegramUserId()).isNull();
        assertThat(response.getChatId()).isNull();
    }

    @Test
    void shouldMapEmptyUserToEmptyResponse() {
        User user = new User();

        UserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getTelegramUserId()).isNull();
        assertThat(response.getChatId()).isNull();
    }

    @Test
    void shouldHandleNonStandardValues() {
        User user = User.builder()
                .telegramUserId(-123456L)
                .chatId(0L)
                .build();

        UserResponse response = mapper.convert(user);

        assertThat(response).isNotNull();
        assertThat(response.getTelegramUserId()).isEqualTo(-123456L);
        assertThat(response.getChatId()).isEqualTo(0L);
    }
}