package ru.mastkey.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mastkey.cloudservice.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.workspaces w WHERE u.id = :id")
    Optional<User> findByUserIdWithWorkspaces(@Param("id") UUID id);

    Optional<User> findByUsername(String username);
}
