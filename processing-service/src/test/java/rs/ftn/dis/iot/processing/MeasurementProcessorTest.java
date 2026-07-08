package rs.ftn.dis.iot.processing;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit testovi za logiku rutiranja u processing-service.
 * ThresholdService je mokovan — testira se samo odluka valid vs breach.
 */
class MeasurementProcessorTest {

    private static final String SENDTO = "spring.cloud.stream.sendto.destination";

    private final ThresholdService thresholdService = mock(ThresholdService.class);
    private final MeasurementProcessor processor = new MeasurementProcessor(thresholdService);

    private Message<Map<String, Object>> process(Map<String, Object> measurement) {
        Function<Map<String, Object>, Message<Map<String, Object>>> fn = processor.processRawMeasurement();
        return fn.apply(measurement);
    }

    private void stub(String deviceId, double co, double temp) {
        when(thresholdService.getThresholds(deviceId))
                .thenReturn(new DeviceThresholds(deviceId, co, temp));
    }

    private Map<String, Object> measurement(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void routesToBreaches_whenCoOverThreshold() {
        stub("dev1", 0.01, 40.0);
        Message<Map<String, Object>> out = process(measurement("deviceId", "dev1", "co", 0.05, "temperature", 20.0));

        assertThat(out.getPayload().get("breach")).isEqualTo(true);
        assertThat(out.getPayload().get("validated")).isEqualTo(true);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("threshold-breaches");
    }

    @Test
    void routesToBreaches_whenTempOverThreshold() {
        stub("dev1", 0.01, 40.0);
        Message<Map<String, Object>> out = process(measurement("deviceId", "dev1", "co", 0.001, "temperature", 45.0));

        assertThat(out.getPayload().get("breach")).isEqualTo(true);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("threshold-breaches");
    }

    @Test
    void routesToValid_whenBelowThresholds() {
        stub("dev1", 0.01, 40.0);
        Message<Map<String, Object>> out = process(measurement("deviceId", "dev1", "co", 0.005, "temperature", 22.0));

        assertThat(out.getPayload().get("breach")).isEqualTo(false);
        assertThat(out.getPayload().get("validated")).isEqualTo(true);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("valid-measurements");
    }

    @Test
    void perDeviceThresholds_sameMeasurementDifferentOutcome() {
        // Bedroom: prag temp 35 -> 37 je breach
        stub("bedroom", 0.01, 35.0);
        Message<Map<String, Object>> bedroom = process(measurement("deviceId", "bedroom", "co", 0.005, "temperature", 37.0));
        assertThat(bedroom.getHeaders().get(SENDTO)).isEqualTo("threshold-breaches");

        // Living room: prag temp 40 -> 37 nije breach
        stub("living", 0.01, 40.0);
        Message<Map<String, Object>> living = process(measurement("deviceId", "living", "co", 0.005, "temperature", 37.0));
        assertThat(living.getHeaders().get(SENDTO)).isEqualTo("valid-measurements");
    }

    @Test
    void missingFields_treatedAsZero_noBreach() {
        stub("dev1", 0.01, 40.0);
        Message<Map<String, Object>> out = process(measurement("deviceId", "dev1"));

        assertThat(out.getPayload().get("breach")).isEqualTo(false);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("valid-measurements");
    }

    @Test
    void nonNumericValues_treatedAsZero_noBreach() {
        stub("dev1", 0.01, 40.0);
        Message<Map<String, Object>> out = process(measurement("deviceId", "dev1", "co", "abc", "temperature", "xyz"));

        assertThat(out.getPayload().get("breach")).isEqualTo(false);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("valid-measurements");
    }

    @Test
    void nullDeviceId_usesReturnedThresholds() {
        when(thresholdService.getThresholds(null)).thenReturn(new DeviceThresholds(null, 0.01, 40.0));
        Message<Map<String, Object>> out = process(measurement("co", 0.05, "temperature", 20.0));

        assertThat(out.getPayload().get("breach")).isEqualTo(true);
        assertThat(out.getHeaders().get(SENDTO)).isEqualTo("threshold-breaches");
    }
}
