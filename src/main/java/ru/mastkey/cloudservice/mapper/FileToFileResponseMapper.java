package ru.mastkey.cloudservice.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;
import ru.mastkey.cloudservice.configuration.MapperConfiguration;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.model.FileResponse;

@Mapper(config = MapperConfiguration.class)
public interface FileToFileResponseMapper extends Converter<File, FileResponse> {

    @BeforeMapping
    default void beforeMapping(@MappingTarget FileResponse target, File source) {
        var fileName = String.format("%s.%s", source.getFileName(), source.getFileExtension());
        target.setFileName(fileName);
    }

    @Override
    @Mapping(target = "fileId", source = "id")
    @Mapping(target = "fileName", ignore = true)
    FileResponse convert(File source);
}
