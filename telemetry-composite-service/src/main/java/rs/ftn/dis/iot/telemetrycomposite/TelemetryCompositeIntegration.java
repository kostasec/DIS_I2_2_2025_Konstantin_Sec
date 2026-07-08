package rs.ftn.dis.iot.telemetrycomposite;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Agregira podatke iz tri nizvodna servisa. Svaki poziv je zaštićen sopstvenim
 * circuit breaker-om (Resilience4j): ako jedan servis padne, njegov deo ide na fallback
 * (prazan/placeholder), a agregat i dalje vraća podatke ostalih servisa (delimičan odgovor).
 */
@Component
public class TelemetryCompositeIntegration {

    private final RestTemplate restTemplate;
    private final CircuitBreaker deviceCb;
    private final CircuitBreaker monitoringCb;
    private final CircuitBreaker alertCb;

    private static final String DEVICE_URL = "http://device-registry-service:8081/device/";
    private static final String MONITORING_URL = "http://monitoring-service:8082/monitoring/";
    private static final String ALERT_URL = "http://alert-service:8083/alert/";

    public TelemetryCompositeIntegration(RestTemplate restTemplate,
                                         CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restTemplate = restTemplate;
        this.deviceCb = circuitBreakerFactory.create("device-registry");
        this.monitoringCb = circuitBreakerFactory.create("monitoring");
        this.alertCb = circuitBreakerFactory.create("alert");
    }

    @SuppressWarnings("unchecked")
    public TelemetryAggregate getAggregate(String deviceId) {
        Map<String, Object> device = deviceCb.run(
                () -> restTemplate.getForObject(DEVICE_URL + deviceId, Map.class),
                t -> Map.of("deviceId", deviceId, "status", "REGISTRY_UNAVAILABLE"));

        Map<String, Object> state = monitoringCb.run(
                () -> restTemplate.getForObject(MONITORING_URL + deviceId + "/state", Map.class),
                t -> Map.of("status", "MONITORING_UNAVAILABLE"));

        List<Map<String, Object>> alerts = alertCb.run(
                () -> restTemplate.getForObject(ALERT_URL + deviceId, List.class),
                t -> List.of());

        return new TelemetryAggregate(device, state, alerts);
    }
}
