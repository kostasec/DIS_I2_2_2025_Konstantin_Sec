package rs.ftn.dis.iot.ingest;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeasurementController {

    private final StreamBridge streamBridge;

    public MeasurementController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestBody Map<String, Object> measurement) {
        streamBridge.send("measurementProducer-out-0", measurement);
        return ResponseEntity.ok("Measurement published to Kafka");
    }
}