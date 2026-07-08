package rs.ftn.dis.iot.deviceregistry;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za DeviceController — DeviceRepository je mokovan.
 */
class DeviceControllerTest {

    private final DeviceRepository repository = mock(DeviceRepository.class);
    private final DeviceController controller = new DeviceController(repository);

    @Test
    void getDevice_found_returns200() {
        DeviceEntity dev = new DeviceEntity("dev1", "Sensor", "Room", "multi");
        when(repository.findById("dev1")).thenReturn(Optional.of(dev));

        ResponseEntity<DeviceEntity> resp = controller.getDevice("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(dev);
    }

    @Test
    void getDevice_notFound_returns404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseEntity<DeviceEntity> resp = controller.getDevice("missing");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getThresholds_usesDeviceValues_whenPresent() {
        DeviceEntity dev = new DeviceEntity("dev1", "Sensor", "Kitchen", "multi", 0.02, 45.0);
        when(repository.findById("dev1")).thenReturn(Optional.of(dev));

        ResponseEntity<DeviceThresholds> resp = controller.getThresholds("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().coThreshold()).isEqualTo(0.02);
        assertThat(resp.getBody().tempThreshold()).isEqualTo(45.0);
    }

    @Test
    void getThresholds_substitutesDefaults_whenNull() {
        DeviceEntity dev = new DeviceEntity("dev1", "Sensor", "Room", "multi"); // pragovi null
        when(repository.findById("dev1")).thenReturn(Optional.of(dev));

        ResponseEntity<DeviceThresholds> resp = controller.getThresholds("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().coThreshold()).isEqualTo(0.01);   // DEFAULT_CO_THRESHOLD
        assertThat(resp.getBody().tempThreshold()).isEqualTo(40.0); // DEFAULT_TEMP_THRESHOLD
    }

    @Test
    void getThresholds_notFound_returns404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseEntity<DeviceThresholds> resp = controller.getThresholds("missing");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createDevice_new_returns201() {
        DeviceEntity dev = new DeviceEntity("dev1", "Sensor", "Room", "multi");
        when(repository.existsById("dev1")).thenReturn(false);
        when(repository.save(dev)).thenReturn(dev);

        ResponseEntity<DeviceEntity> resp = controller.createDevice(dev);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isSameAs(dev);
        verify(repository).save(dev);
    }

    @Test
    void createDevice_duplicate_returns400() {
        DeviceEntity dev = new DeviceEntity("dev1", "Sensor", "Room", "multi");
        when(repository.existsById("dev1")).thenReturn(true);

        ResponseEntity<DeviceEntity> resp = controller.createDevice(dev);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any());
    }
}
