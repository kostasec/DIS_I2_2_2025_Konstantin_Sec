package rs.ftn.dis.iot.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Configuration
public class MeasurementConsumer {

    private final Map<String, Map<String, Object>> deviceStates = new ConcurrentHashMap<>();

    @Bean
    public Consumer<Map<String, Object>> stateUpdater() {
        return measurement -> {
            String deviceId = (String) measurement.get("deviceId");
            if (deviceId != null) {
                deviceStates.put(deviceId, measurement);
                System.out.println("Updated state for device: " + deviceId);
            }
        };
    }

    public Map<String, Object> getState(String deviceId) {
        return deviceStates.get(deviceId);
    }
}