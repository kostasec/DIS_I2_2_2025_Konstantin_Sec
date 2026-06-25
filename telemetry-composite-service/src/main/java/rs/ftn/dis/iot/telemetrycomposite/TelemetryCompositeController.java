package rs.ftn.dis.iot.telemetrycomposite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelemetryCompositeController {

    private final TelemetryCompositeIntegration integration;

    public TelemetryCompositeController(TelemetryCompositeIntegration integration) {
        this.integration = integration;
    }

    @GetMapping("/telemetry/{deviceId}")
    public ResponseEntity<TelemetryAggregate> getTelemetry(@PathVariable String deviceId) {
        return ResponseEntity.ok(integration.getAggregate(deviceId));
    }
}