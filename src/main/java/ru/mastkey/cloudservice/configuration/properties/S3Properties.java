package ru.mastkey.cloudservice.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("s3")
@Data
public class S3Properties {
    private String url;
    private String accessKey;
    private String secretKey;
}