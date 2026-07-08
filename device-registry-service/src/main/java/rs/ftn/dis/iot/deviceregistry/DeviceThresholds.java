package rs.ftn.dis.iot.deviceregistry;

/**
 * Pragovi za alarmiranje po uređaju — kontrakt koji processing-service
 * sinhrono povlači (GET /device/{id}/thresholds).
 */
public record DeviceThresholds(String deviceId, double coThreshold, double tempThreshold) {
}
