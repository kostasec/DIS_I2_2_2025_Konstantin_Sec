package rs.ftn.dis.iot.alert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class AlertController {

    @GetMapping("/alert/{deviceId}")
    public ResponseEntity<List<Map<String, Object>>> getAlertsForDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(List.of(
            Map.of(
                "alertId", "alert-001",
                "deviceId", deviceId,
                "type", "CO_THRESHOLD",
                "message", "CO level exceeded threshold",
                "status", "ACTIVE",
                "createdAt", Instant.now().toString()
            )
        ));
    }
}