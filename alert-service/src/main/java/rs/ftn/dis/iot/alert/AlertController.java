package rs.ftn.dis.iot.alert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertController {

    private final AlertConsumer consumer;

    public AlertController(AlertConsumer consumer) {
        this.consumer = consumer;
    }

    @GetMapping("/alert/{deviceId}")
    public ResponseEntity<List<Object>> getAlertsForDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(consumer.getAlerts(deviceId));
    }
}