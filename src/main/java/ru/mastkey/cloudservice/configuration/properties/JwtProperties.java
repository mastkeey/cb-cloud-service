package ru.mastkey.cloudservice.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "jwt")
@Component
public class JwtProperties {
    private Integer ttlInMin;
    private String secretKey;
}
