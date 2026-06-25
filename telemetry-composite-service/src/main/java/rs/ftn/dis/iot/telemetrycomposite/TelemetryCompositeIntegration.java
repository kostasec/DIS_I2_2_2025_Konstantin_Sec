package rs.ftn.dis.iot.telemetrycomposite;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class TelemetryCompositeIntegration {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DEVICE_URL = "http://device-registry-service:8081/device/";
    private static final String MONITORING_URL = "http://monitoring-service:8082/monitoring/";
    private static final String ALERT_URL = "http://alert-service:8083/alert/";

    public TelemetryAggregate getAggregate(String deviceId) {
        Map device = restTemplate.getForObject(DEVICE_URL + deviceId, Map.class);
        Map state = restTemplate.getForObject(MONITORING_URL + deviceId + "/state", Map.class);
        List alerts = restTemplate.getForObject(ALERT_URL + deviceId, List.class);
        return new TelemetryAggregate(device, state, alerts);
    }
}
