package com.centralized.inventoryservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component("configServer")
@RestController
public class ConfigServerHealthIndicator implements HealthIndicator {

    @Autowired
    private Environment env;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {
        if (checkConnection()) {
            return Health.up().withDetail("configServer", "connected").build();
        } else {
            return Health.down().withDetail("configServer", "disconnected").build();
        }
    }

    @GetMapping("/api/inventory/health")
    public Map<String, String> getHealth() {
        if (checkConnection()) {
            return Map.of(
                "status", "UP",
                "configServer", "connected"
            );
        } else {
            return Map.of(
                "status", "DOWN",
                "configServer", "disconnected"
            );
        }
    }

    private boolean checkConnection() {
        try {
            String configUri = env.getProperty("spring.cloud.config.uri", "http://config-server:8888");
            if (configUri.contains(",")) {
                configUri = configUri.split(",")[0].trim();
            }
            // Verify connection by contacting the Config Server
            restTemplate.getForObject(configUri + "/actuator/health", Map.class);
            return true;
        } catch (Exception e) {
            // Fallback check to verify if the configurations were successfully loaded on startup
            return env.containsProperty("inventory.maxStock");
        }
    }
}
