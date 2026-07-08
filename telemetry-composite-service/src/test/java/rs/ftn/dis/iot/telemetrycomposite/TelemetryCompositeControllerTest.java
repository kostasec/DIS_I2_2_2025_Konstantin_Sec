package rs.ftn.dis.iot.telemetrycomposite;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test za TelemetryCompositeController — integracioni sloj je mokovan.
 */
class TelemetryCompositeControllerTest {

    private final TelemetryCompositeIntegration integration = mock(TelemetryCompositeIntegration.class);
    private final TelemetryCompositeController controller = new TelemetryCompositeController(integration);

    @Test
    void returnsAggregate() {
        TelemetryAggregate aggregate = new TelemetryAggregate(
                Map.of("deviceId", "dev1"),
                Map.of("temperature", 22.5),
                List.of(Map.of("alertId", "alert-1")));
        when(integration.getAggregate("dev1")).thenReturn(aggregate);

        ResponseEntity<TelemetryAggregate> resp = controller.getTelemetry("dev1");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(aggregate);
    }
}
