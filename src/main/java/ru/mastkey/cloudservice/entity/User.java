package ru.mastkey.cloudservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "tg_user")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "bucket_name", nullable = false)
    private String bucketName;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @ManyToMany
    @JoinTable(
            name = "user_workspace",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "workspace_id")
    )
    private List<Workspace> workspaces = new ArrayList<>();

    @PrePersist
    void prePersist() {
        var uuid = UUID.randomUUID();
        this.id = uuid;
        this.bucketName = uuid.toString();
        this.createdAt = LocalDateTime.now();
    }
}