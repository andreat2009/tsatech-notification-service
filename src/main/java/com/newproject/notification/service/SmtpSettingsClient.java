package com.newproject.notification.service;

import com.newproject.notification.dto.SmtpRuntimeSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SmtpSettingsClient {
    private static final Logger logger = LoggerFactory.getLogger(SmtpSettingsClient.class);

    private final RestTemplate restTemplate;
    private final String runtimeUrl;
    private final String internalToken;

    public SmtpSettingsClient(
        @Value("${cms.settings.runtime-url:}") String runtimeUrl,
        @Value("${cms.settings.internal-token:}") String internalToken
    ) {
        this.restTemplate = new RestTemplate();
        this.runtimeUrl = runtimeUrl;
        this.internalToken = internalToken;
    }

    public SmtpRuntimeSettings fetchRuntimeSettings() {
        if (runtimeUrl == null || runtimeUrl.isBlank()) {
            logger.warn("Unable to load SMTP runtime settings: cms.settings.runtime-url is not configured");
            return null;
        }
        if (internalToken == null || internalToken.isBlank()) {
            logger.warn("Unable to load SMTP runtime settings: cms.settings.internal-token is not configured");
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<SmtpRuntimeSettings> response = restTemplate.exchange(
                runtimeUrl,
                HttpMethod.GET,
                entity,
                SmtpRuntimeSettings.class
            );
            return response.getBody();
        } catch (Exception ex) {
            logger.warn("Unable to load SMTP runtime settings from {}: {}", runtimeUrl, ex.getMessage());
            return null;
        }
    }
}
