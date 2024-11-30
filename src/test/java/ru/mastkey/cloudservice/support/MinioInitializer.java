package ru.mastkey.cloudservice.support;

import lombok.SneakyThrows;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MinIOContainer;

public class MinioInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final MinIOContainer minioServer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");

    @Override
    @SneakyThrows
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String accessKey = "minioAccessKey";
        String secretKey = "minioSecretKey";

        minioServer
                .withEnv("MINIO_ACCESS_KEY", accessKey)
                .withEnv("MINIO_SECRET_KEY", secretKey)
                .start();

        TestPropertyValues.of(
                "s3.url=" + minioServer.getS3URL(),
                "s3.accessKey=" + accessKey,
                "s3.secretKey=" + secretKey
        ).applyTo(applicationContext.getEnvironment());
    }
}
