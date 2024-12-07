package ru.mastkey.cloudservice.mapper;

import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;
import ru.mastkey.cloudservice.configuration.MapperConfiguration;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.CreateUserRequest;

@Mapper(config = MapperConfiguration.class)
public interface CreateUserRequestToUserMapper extends Converter<CreateUserRequest, User> {

    @Override
    User convert(CreateUserRequest source);
}
