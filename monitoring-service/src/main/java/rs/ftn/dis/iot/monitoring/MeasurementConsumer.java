package rs.ftn.dis.iot.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class MeasurementConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public MeasurementConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Bean
    public Consumer<Map<String, Object>> stateUpdater() {
        return measurement -> {
            String deviceId = (String) measurement.get("deviceId");
            if (deviceId != null) {
                redisTemplate.opsForHash().putAll("device:state:" + deviceId, measurement);
                System.out.println("Updated state for device: " + deviceId);
            }
        };
    }

    public Map<Object, Object> getState(String deviceId) {
        return redisTemplate.opsForHash().entries("device:state:" + deviceId);
    }
}