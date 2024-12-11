package ru.mastkey.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mastkey.cloudservice.entity.UserWorkspace;

import java.util.Optional;
import java.util.UUID;

public interface UserWorkspaceRepository extends JpaRepository<UserWorkspace, UUID> {
    Optional<UserWorkspace> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

    @Modifying
    @Query("DELETE FROM UserWorkspace uw WHERE uw.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Modifying
    @Query("DELETE FROM UserWorkspace uw WHERE uw.user.id = :userId AND uw.workspace.id = :workspaceId")
    void deleteByUserIdAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);
}
