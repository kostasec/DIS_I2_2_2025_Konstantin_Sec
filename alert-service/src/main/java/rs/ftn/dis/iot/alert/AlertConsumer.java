package rs.ftn.dis.iot.alert;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Configuration
public class AlertConsumer {

    private final Map<String, List<Map<String, Object>>> deviceAlerts = new ConcurrentHashMap<>();

    @Bean
    public Consumer<Map<String, Object>> alertHandler() {
        return measurement -> {
            String deviceId = (String) measurement.get("deviceId");
            if (deviceId == null) return;

            Map<String, Object> alert = new HashMap<>();
            alert.put("alertId", "alert-" + UUID.randomUUID().toString().substring(0, 8));
            alert.put("deviceId", deviceId);
            alert.put("type", "THRESHOLD_BREACH");
            alert.put("message", "Sensor value exceeded threshold");
            alert.put("status", "ACTIVE");
            alert.put("createdAt", Instant.now().toString());
            alert.put("measurement", measurement);

            deviceAlerts.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(alert);
            System.out.println("Alert created for device: " + deviceId);
        };
    }

    public List<Map<String, Object>> getAlerts(String deviceId) {
        return deviceAlerts.getOrDefault(deviceId, List.of());
    }
}