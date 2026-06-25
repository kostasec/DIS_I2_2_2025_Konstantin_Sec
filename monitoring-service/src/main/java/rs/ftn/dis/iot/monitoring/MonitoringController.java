package rs.ftn.dis.iot.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class MonitoringController {

    @GetMapping("/monitoring/{deviceId}/state")
    public ResponseEntity<Map<String, Object>> getDeviceState(@PathVariable String deviceId) {
        return ResponseEntity.ok(Map.of(
            "deviceId", deviceId,
            "temperature", 22.5,
            "humidity", 55.0,
            "co", 0.004,
            "status", "NORMAL",
            "lastUpdated", Instant.now().toString()
        ));
    }
}