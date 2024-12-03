package ru.mastkey.cloudservice.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.model.CreateUserResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserToCreateUserResponseMapper extends Converter<User, CreateUserResponse> {
    @BeforeMapping
    default void beforeMapping(@MappingTarget CreateUserResponse target, User source) {
        target.setWorkspaceId(source.getWorkspaces().get(0).getId());
    }

    @Override
    CreateUserResponse convert(User source);
}
