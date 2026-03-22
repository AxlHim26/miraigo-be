package com.example.japanweb.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.email-verification")
public class EmailVerificationProperties {

    @NotNull
    private Duration tokenExpiration = Duration.ofMinutes(30);

    @NotBlank
    private String webBaseUrl = "http://localhost:3000";

    @NotBlank
    private String verificationPath = "/verify-email";

    @NotBlank
    private String fromAddress = "no-reply@miraigo.local";
}
