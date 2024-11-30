package ru.mastkey.cloudservice.util;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.Workspace;

import java.util.UUID;

public class SpecificationUtils {
    public static Specification<Workspace> getWorkspacesSpecification(Long telegramUserId) {
        return Specification.where((root, query, criteriaBuilder) -> {
            Join<Workspace, User> userJoin = root.join("user");
            var userCondition = criteriaBuilder.equal(userJoin.get("telegramUserId"), telegramUserId);
            return criteriaBuilder.and(userCondition);
        });
    }

    public static Specification<File> getFilesSpecification(UUID workspaceId) {
        return Specification.where((root, query, criteriaBuilder) -> {
            Join<File, Workspace> workspaceJoin = root.join("workspace");
            var workspaceCondition = criteriaBuilder.equal(workspaceJoin.get("id"), workspaceId);
            return criteriaBuilder.and(workspaceCondition);
        });
    }
}
