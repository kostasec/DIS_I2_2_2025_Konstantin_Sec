package rs.ftn.dis.iot.processing;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sinhrono povlači pragove po uređaju iz device-registry-service i kešira ih.
 * Poziv je zaštićen circuit breaker-om (Resilience4j): ako registry pada ili je nedostupan,
 * kolo se otvara i odmah se vraćaju podrazumevani pragovi (fallback), bez čekanja na timeout.
 */
@Service
public class ThresholdService {

    static final double DEFAULT_CO_THRESHOLD = 0.01;
    static final double DEFAULT_TEMP_THRESHOLD = 40.0;

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, DeviceThresholds> cache = new ConcurrentHashMap<>();

    public ThresholdService(RestClient.Builder loadBalancedRestClientBuilder,
                            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://device-registry-service")
                .build();
        this.circuitBreaker = circuitBreakerFactory.create("deviceRegistry");
    }

    public DeviceThresholds getThresholds(String deviceId) {
        if (deviceId == null) {
            return defaults(null);
        }
        DeviceThresholds cached = cache.get(deviceId);
        if (cached != null) {
            return cached;
        }
        return circuitBreaker.run(
                () -> fetchFromRegistry(deviceId),
                throwable -> {
                    System.out.println("Threshold fetch failed for " + deviceId
                            + ", using defaults: " + throwable.getMessage());
                    return defaults(deviceId);
                });
    }

    private DeviceThresholds fetchFromRegistry(String deviceId) {
        DeviceThresholds fetched = restClient.get()
                .uri("/device/{id}/thresholds", deviceId)
                .retrieve()
                .body(DeviceThresholds.class);
        DeviceThresholds result = (fetched != null) ? fetched : defaults(deviceId);
        cache.put(deviceId, result);
        return result;
    }

    private DeviceThresholds defaults(String deviceId) {
        return new DeviceThresholds(deviceId, DEFAULT_CO_THRESHOLD, DEFAULT_TEMP_THRESHOLD);
    }
}
