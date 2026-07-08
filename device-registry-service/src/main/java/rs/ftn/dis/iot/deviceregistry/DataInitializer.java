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
        // Uređaji iz dataset-a, svaki sa sopstvenim pragovima za alarmiranje.
        // Kuhinja toleriše više CO i topline (kuvanje); spavaća soba je stroža na temperaturu.
        seed("b8:27:eb:bf:9d:51", "Living Room Sensor", "Living Room", "multi", 0.01, 40.0);
        seed("00:0f:00:70:91:0a", "Kitchen Sensor",     "Kitchen",     "multi", 0.02, 45.0);
        seed("1c:bf:ce:15:ec:4d", "Bedroom Sensor",     "Bedroom",     "multi", 0.01, 35.0);
    }

    private void seed(String id, String name, String location, String type,
                      double coThreshold, double tempThreshold) {
        DeviceEntity device = deviceRepository.findById(id).orElse(null);
        if (device == null) {
            deviceRepository.save(new DeviceEntity(id, name, location, type, coThreshold, tempThreshold));
            System.out.println("Initialized device: " + id);
        } else if (device.getCoThreshold() == null || device.getTempThreshold() == null) {
            // Backfill pragova za uređaje registrovane pre uvođenja ovog polja.
            device.setCoThreshold(coThreshold);
            device.setTempThreshold(tempThreshold);
            deviceRepository.save(device);
            System.out.println("Backfilled thresholds for device: " + id);
        }
    }
}
