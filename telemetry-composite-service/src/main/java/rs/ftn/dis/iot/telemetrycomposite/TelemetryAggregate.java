package rs.ftn.dis.iot.telemetrycomposite;

import java.util.Map;
import java.util.List;

public class TelemetryAggregate {
    private Map<String, Object> device;
    private Map<String, Object> state;
    private List<Map<String, Object>> alerts;

    public TelemetryAggregate(Map<String, Object> device, Map<String, Object> state, List<Map<String, Object>> alerts) {
        this.device = device;
        this.state = state;
        this.alerts = alerts;
    }

    public Map<String, Object> getDevice() { return device; }
    public Map<String, Object> getState() { return state; }
    public List<Map<String, Object>> getAlerts() { return alerts; }
}