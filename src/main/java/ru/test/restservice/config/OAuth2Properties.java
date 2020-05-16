package ru.test.restservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("spring.security.oauth2.client.registration.spbstu")
public class OAuth2Properties {
    public String clientId;
    public String clientSecret;
}
