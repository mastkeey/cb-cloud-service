package ru.mastkey.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mastkey.cloudservice.entity.File;
import ru.mastkey.cloudservice.entity.Workspace;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID>, JpaSpecificationExecutor<File> {
    Optional<File> findByWorkspaceAndFileNameAndFileExtension(Workspace workspace, String fileName, String fileExtension);
}
