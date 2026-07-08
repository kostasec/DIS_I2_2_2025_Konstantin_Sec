package rs.ftn.dis.iot.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za MonitoringController — MeasurementConsumer je mokovan.
 */
class MonitoringControllerTest {

    private final MeasurementConsumer consumer = mock(MeasurementConsumer.class);
    private final MonitoringController controller = new MonitoringController(consumer);

    @Test
    void returnsState_whenPresent() {
        Map<Object, Object> state = Map.of("temperature", 22.0, "breach", false);
        when(consumer.getState("dev1")).thenReturn(state);

        ResponseEntity<Map<Object, Object>> resp = controller.getDeviceState("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(state);
    }

    @Test
    void returnsNoData_whenNull() {
        when(consumer.getState("dev1")).thenReturn(null);

        ResponseEntity<Map<Object, Object>> resp = controller.getDeviceState("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "NO_DATA");
        assertThat(resp.getBody()).containsEntry("deviceId", "dev1");
    }

    @Test
    void returnsNoData_whenEmpty() {
        when(consumer.getState("dev1")).thenReturn(Map.of());

        ResponseEntity<Map<Object, Object>> resp = controller.getDeviceState("dev1");

        assertThat(resp.getBody()).containsEntry("status", "NO_DATA");
    }
}
