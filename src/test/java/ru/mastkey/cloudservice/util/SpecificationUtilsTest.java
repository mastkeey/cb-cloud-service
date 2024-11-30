package ru.mastkey.cloudservice.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SpecificationUtilsTest {

    @Test
    void classTest() {
        var specificationUtils = new SpecificationUtils();
        assertThat(specificationUtils).isNotNull();
    }

    @Test
    void getWorkspacesSpecification_ShouldCreateSpecificationSuccessfully() {
        Long telegramUserId = 12345L;
        Root<Workspace> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Join<Workspace, User> join = mock(Join.class);

        when(root.<Workspace, User>join("user")).thenReturn(join);
        when(criteriaBuilder.equal(join.get("telegramUserId"), telegramUserId)).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));

        Specification<Workspace> specification = SpecificationUtils.getWorkspacesSpecification(telegramUserId);
        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).join("user");
        verify(criteriaBuilder).equal(join.get("telegramUserId"), telegramUserId);
    }

    @Test
    void getFilesSpecification_ShouldCreateSpecificationSuccessfully() {
        UUID workspaceId = UUID.randomUUID();
        Root<File> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Join<File, Workspace> join = mock(Join.class);

        when(root.<File, Workspace>join("workspace")).thenReturn(join);
        when(criteriaBuilder.equal(join.get("id"), workspaceId)).thenReturn(mock(jakarta.persistence.criteria.Predicate.class));

        Specification<File> specification = SpecificationUtils.getFilesSpecification(workspaceId);
        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).join("workspace");
        verify(criteriaBuilder).equal(join.get("id"), workspaceId);
    }
}