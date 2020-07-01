package ru.test.restservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("admin")
public class AdminProperties {
    public int idleTimeout;
    public int maxProjectCount;
    public int compilationInterval;
    public int maxPathLength;
}
