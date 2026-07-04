package rs.ftn.dis.iot.deviceregistry;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DeviceRepository deviceRepository;

    public DataInitializer(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void run(String... args) {
        saveIfAbsent("sensor-009", "Smoke Detector", "Server Room", "smoke");
        saveIfAbsent("b8:27:eb:bf:9d:51", "Living Room Sensor", "Living Room", "multi");
        saveIfAbsent("00:0f:00:70:91:0a", "Kitchen Sensor", "Kitchen", "multi");
        saveIfAbsent("1c:bf:ce:15:ec:4d", "Bedroom Sensor", "Bedroom", "multi");
    }

    private void saveIfAbsent(String id, String name, String location, String type) {
        if (!deviceRepository.existsById(id)) {
            DeviceEntity device = new DeviceEntity(id, name, location, type);
            deviceRepository.save(device);
            System.out.println("Initialized device: " + id);
        }
    }
}