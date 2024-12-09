package ru.mastkey.cloudservice.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;
import ru.mastkey.cloudservice.security.JwtService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.profiles.active=test"})
@ContextConfiguration(initializers = {PostgreSQLInitializer.class, MinioInitializer.class})
@TestPropertySource(locations = "classpath:application-test.yml")
public class IntegrationTestBase {
    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    protected UserWorkspaceRepository userWorkspaceRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected WorkspaceRepository workspaceRepository;

    @Autowired
    protected FileRepository fileRepository;

    @Autowired
    protected S3Client s3Client;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User createUser() {
        var user = new User();
        user.setUsername("mastkeyy");
        user.setPassword(passwordEncoder.encode("mastkey"));
        userRepository.save(user);
        s3Client.createBucketIfNotExists(user.getBucketName());
        return user;
    }

    protected Workspace createWorkspaceWithUser() {
        var user = userRepository.save(User.builder()
                .bucketName("mastkey-1")
                .workspaces(new ArrayList<>())
                .username("mastkey")
                .password(passwordEncoder.encode("mastkey"))
                .build());

        var workspace = Workspace.builder()
                .name("mastkey")
                .owner(user)
                .build();

        workspace = workspaceRepository.save(workspace);
        workspace.setUsers(List.of(user));

        userRepository.save(user);

        var userWorkspace = new UserWorkspace();
        userWorkspace.setUser(user);
        userWorkspace.setWorkspace(workspace);
        userWorkspaceRepository.save(userWorkspace);

        s3Client.createBucketIfNotExists(user.getBucketName());

        return workspace;
    }

    protected String createTokenForSavedUser(User user) {
        return jwtService.generateToken(user);
    }

    protected MultiValueMap<String, String> createAuthHeader(String token) {
        var value = "Bearer %s".formatted(token);
        return new HttpHeaders() {{
            put("Authorization", List.of(value));
        }};
    }

    @AfterEach
    public void tearDown() {
        fileRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }
}
