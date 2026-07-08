package rs.ftn.dis.iot.deviceregistry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceController {

    // Podrazumevani pragovi kada uređaj nema definisane sopstvene.
    private static final double DEFAULT_CO_THRESHOLD = 0.01;
    private static final double DEFAULT_TEMP_THRESHOLD = 40.0;

    private final DeviceRepository repository;

    public DeviceController(DeviceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/device/{id}")
    public ResponseEntity<DeviceEntity> getDevice(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/device/{id}/thresholds")
    public ResponseEntity<DeviceThresholds> getThresholds(@PathVariable String id) {
        return repository.findById(id)
                .map(d -> new DeviceThresholds(
                        d.getDeviceId(),
                        d.getCoThreshold() != null ? d.getCoThreshold() : DEFAULT_CO_THRESHOLD,
                        d.getTempThreshold() != null ? d.getTempThreshold() : DEFAULT_TEMP_THRESHOLD))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/device")
    public ResponseEntity<DeviceEntity> createDevice(@RequestBody DeviceEntity device) {
        if (repository.existsById(device.getDeviceId())) {
            return ResponseEntity.badRequest().build();
        }
        DeviceEntity saved = repository.save(device);
        return ResponseEntity.status(201).body(saved);
    }
}