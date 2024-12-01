package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.mastkey.model.FileResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageToPageFileResponseMapperTest {
    private PageToPageFileResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(PageToPageFileResponseMapper.class);
    }

    @Test
    void toResponse() {
        var fileResponse1 = new FileResponse();
        fileResponse1.setFileName("file1");
        var fileResponse2 = new FileResponse();
        fileResponse2.setFileName("file2");

        var files = List.of(fileResponse1, fileResponse2);

        var page = new PageImpl<>(files, PageRequest.of(0,2),2);

        var pagedFiles = mapper.convert(page);

        assertThat(pagedFiles.getContent().size()).isEqualTo(page.getContent().size());
        assertThat(pagedFiles.getContent().get(0).getFileName()).isEqualTo(page.getContent().get(0).getFileName());
        assertThat(pagedFiles.getContent().get(1).getFileName()).isEqualTo(page.getContent().get(1).getFileName());
        assertThat(pagedFiles.getTotalPages()).isEqualTo(page.getTotalPages());
        assertThat(pagedFiles.getTotalElements()).isEqualTo(page.getTotalElements());
    }
}