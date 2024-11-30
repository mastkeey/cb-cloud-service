package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mastkey.cloudservice.entity.File;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileToFileResponseMapperTest {

    private FileToFileResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(FileToFileResponseMapper.class);
    }

    @Test
    void beforeMapping_ShouldFormatFileNameCorrectly() {
        var source = new File();
        source.setFileName("document");
        source.setFileExtension("pdf");

        var result = mapper.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("document.pdf");
    }

    @Test
    void shouldMapFileIdCorrectly() {
        var fileId = UUID.randomUUID();
        var source = new File();
        source.setId(fileId);

        var result = mapper.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo(fileId);
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        var source = new File();

        var result = mapper.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isNull();
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        var fileId = UUID.randomUUID();
        var source = new File();
        source.setId(fileId);
        source.setFileName("image");
        source.setFileExtension("jpg");

        var result = mapper.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getFileId()).isEqualTo(fileId);
        assertThat(result.getFileName()).isEqualTo("image.jpg");
    }
}