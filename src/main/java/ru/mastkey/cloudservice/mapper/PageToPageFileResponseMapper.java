package ru.mastkey.cloudservice.mapper;

import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import ru.mastkey.cloudservice.configuration.MapperConfiguration;
import ru.mastkey.model.FileResponse;
import ru.mastkey.model.PageFileResponse;

@Mapper(config = MapperConfiguration.class)
public interface PageToPageFileResponseMapper extends Converter<Page<FileResponse>, PageFileResponse> {
    @Override
    PageFileResponse convert(Page<FileResponse> source);
}
