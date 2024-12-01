package ru.mastkey.cloudservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.mastkey.model.WorkspaceResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageToPageWorkspaceResponseMapperTest {
    private PageToPageWorkspaceResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(PageToPageWorkspaceResponseMapper.class);
    }

    @Test
    void toResponse() {
        var workspaceResponse1 = new WorkspaceResponse();
        workspaceResponse1.setName("space1");
        var workspaceResponse2 = new WorkspaceResponse();
        workspaceResponse2.setName("space2");

        var files = List.of(workspaceResponse1, workspaceResponse2);

        var page = new PageImpl<>(files, PageRequest.of(0,2),2);

        var pagedSpaces = mapper.convert(page);

        assertThat(pagedSpaces.getContent().size()).isEqualTo(page.getContent().size());
        assertThat(pagedSpaces.getContent().get(0).getName()).isEqualTo(page.getContent().get(0).getName());
        assertThat(pagedSpaces.getContent().get(1).getName()).isEqualTo(page.getContent().get(1).getName());
        assertThat(pagedSpaces.getTotalPages()).isEqualTo(page.getTotalPages());
        assertThat(pagedSpaces.getTotalElements()).isEqualTo(page.getTotalElements());
    }
}