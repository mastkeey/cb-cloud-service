package ru.mastkey.cloudservice.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
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
        UUID userId = UUID.randomUUID();

        Root<Workspace> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Join<Workspace, User> userJoin = mock(Join.class);
        jakarta.persistence.criteria.Predicate userCondition = mock(jakarta.persistence.criteria.Predicate.class);

        when(root.<Workspace, User>join("users")).thenReturn(userJoin);
        when(criteriaBuilder.equal(userJoin.get("id"), userId)).thenReturn(userCondition);

        Specification<Workspace> specification = SpecificationUtils.getWorkspacesSpecification(userId);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(root).join("users");
        verify(criteriaBuilder).equal(userJoin.get("id"), userId);
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