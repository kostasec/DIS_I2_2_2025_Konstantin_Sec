package rs.ftn.dis.iot.alert;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za AlertController — AlertConsumer je mokovan.
 */
class AlertControllerTest {

    private final AlertConsumer consumer = mock(AlertConsumer.class);
    private final AlertController controller = new AlertController(consumer);

    @Test
    void returnsAlerts_whenPresent() {
        List<Object> alerts = List.of(Map.of("alertId", "alert-123", "status", "ACTIVE"));
        when(consumer.getAlerts("dev1")).thenReturn(alerts);

        ResponseEntity<List<Object>> resp = controller.getAlertsForDevice("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(alerts);
    }

    @Test
    void returnsEmptyList_whenNoAlerts() {
        when(consumer.getAlerts("dev1")).thenReturn(List.of());

        ResponseEntity<List<Object>> resp = controller.getAlertsForDevice("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }
}
