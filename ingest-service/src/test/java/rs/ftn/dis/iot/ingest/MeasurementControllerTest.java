package rs.ftn.dis.iot.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test za MeasurementController — StreamBridge je mokovan.
 */
class MeasurementControllerTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final MeasurementController controller = new MeasurementController(streamBridge);

    @Test
    void ingest_publishesToKafkaBinding_andReturns200() {
        Map<String, Object> measurement = Map.of("deviceId", "dev1", "temperature", 22.0);

        ResponseEntity<String> resp = controller.ingest(measurement);

        verify(streamBridge).send("measurementProducer-out-0", measurement);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("Measurement published to Kafka");
    }
}
