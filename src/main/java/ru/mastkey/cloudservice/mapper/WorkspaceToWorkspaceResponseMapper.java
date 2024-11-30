package ru.mastkey.cloudservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.core.convert.converter.Converter;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.model.WorkspaceResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkspaceToWorkspaceResponseMapper extends Converter<Workspace, WorkspaceResponse> {
    @Override
    @Mapping(target = "telegramUserId", source = "user.telegramUserId")
    @Mapping(target = "workspaceId", source = "id")
    WorkspaceResponse convert(Workspace source);
}
