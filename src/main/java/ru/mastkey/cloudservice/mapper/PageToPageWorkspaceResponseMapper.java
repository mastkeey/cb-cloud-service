package ru.mastkey.cloudservice.mapper;

import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import ru.mastkey.cloudservice.configuration.MapperConfiguration;
import ru.mastkey.model.PageWorkspaceResponse;
import ru.mastkey.model.WorkspaceResponse;

@Mapper(config = MapperConfiguration.class)
public interface PageToPageWorkspaceResponseMapper extends Converter<Page<WorkspaceResponse>, PageWorkspaceResponse> {
    @Override
    PageWorkspaceResponse convert(Page<WorkspaceResponse> source);
}
