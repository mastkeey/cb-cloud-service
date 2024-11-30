package ru.mastkey.cloudservice.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "properties")
@Component
public class Properties {
    private Integer pageSize;
}
