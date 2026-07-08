package rs.ftn.dis.iot.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class MonitoringController {

    private final MeasurementConsumer consumer;

    public MonitoringController(MeasurementConsumer consumer) {
        this.consumer = consumer;
    }

    @GetMapping("/monitoring/{deviceId}/state")
    public ResponseEntity<Map<Object, Object>> getDeviceState(@PathVariable String deviceId) {
        Map<Object, Object> state = consumer.getState(deviceId);
        if (state == null || state.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "status", "NO_DATA",
                "lastUpdated", Instant.now().toString()
            ));
        }
        return ResponseEntity.ok(state);
    }
}