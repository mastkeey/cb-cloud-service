package ru.mastkey.cloudservice.configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mastkey.cloudservice.configuration.properties.S3Properties;

@Configuration
@RequiredArgsConstructor
public class MinioClientConfiguration {

    private final S3Properties s3Properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(s3Properties.getUrl())
                .credentials(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                .build();
    }
}
