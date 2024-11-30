package ru.mastkey.cloudservice.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;
import ru.mastkey.cloudservice.configuration.MapperConfiguration;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.CreateUserRequest;

@Mapper(config = MapperConfiguration.class)
public interface CreateUserRequestToUserMapper extends Converter<CreateUserRequest, User> {

    @BeforeMapping
    default void beforeMapping(@MappingTarget User user,  CreateUserRequest source) {
        user.setBucketName(String.valueOf(source.getTelegramUserId()));
    }

    @Override
    User convert(CreateUserRequest source);
}
