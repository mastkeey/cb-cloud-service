package ru.mastkey.cloudservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mastkey.cloudservice.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTelegramUserId(Long telegramUserId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.workspaces WHERE u.telegramUserId = :telegramUserId")
    Optional<User> findByTelegramUserIdWithWorkspaces(@Param("telegramUserId") Long telegramUserId);
}
