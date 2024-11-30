package ru.mastkey.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mastkey.cloudservice.entity.Workspace;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID>, JpaSpecificationExecutor<Workspace> {

    @Query("SELECT w FROM Workspace w LEFT JOIN FETCH w.files WHERE w.id = :id")
    Optional<Workspace> findByIdWithFiles(@Param("id") UUID id);
}
