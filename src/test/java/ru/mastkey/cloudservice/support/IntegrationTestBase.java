package ru.mastkey.cloudservice.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mastkey.cloudservice.client.S3Client;
import ru.mastkey.cloudservice.entity.User;
import ru.mastkey.cloudservice.entity.UserWorkspace;
import ru.mastkey.cloudservice.entity.Workspace;
import ru.mastkey.cloudservice.repository.FileRepository;
import ru.mastkey.cloudservice.repository.UserRepository;
import ru.mastkey.cloudservice.repository.UserWorkspaceRepository;
import ru.mastkey.cloudservice.repository.WorkspaceRepository;

import java.util.ArrayList;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    protected User createUser() {
        var user = new User();
        user.setBucketName("test");
        s3Client.createBucketIfNotExists(user.getBucketName());
        return userRepository.save(user);
    }

    protected Workspace createWorkspaceWithUser() {
        var user = userRepository.save(User.builder()
                .bucketName("mastkey-1")
                .workspaces(new ArrayList<>())
                .build());

        var workspace = Workspace.builder()
                .name("mastkey")
                .build();

        workspace = workspaceRepository.save(workspace);

        userRepository.save(user);

        var userWorkspace = new UserWorkspace();
        userWorkspace.setUser(user);
        userWorkspace.setWorkspace(workspace);
        userWorkspaceRepository.save(userWorkspace);

        s3Client.createBucketIfNotExists(user.getBucketName());

        return workspace;
    }

    @AfterEach
    public void tearDown() {
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        fileRepository.deleteAll();
    }
}
