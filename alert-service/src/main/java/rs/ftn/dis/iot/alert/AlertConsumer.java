package rs.ftn.dis.iot.alert;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@Configuration
public class AlertConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public AlertConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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

            redisTemplate.opsForList().rightPush("device:alerts:" + deviceId, alert);
            System.out.println("Alert created for device: " + deviceId);
        };
    }

    public List<Object> getAlerts(String deviceId) {
        List<Object> alerts = redisTemplate.opsForList().range("device:alerts:" + deviceId, 0, -1);
        return alerts != null ? alerts : List.of();
    }
}