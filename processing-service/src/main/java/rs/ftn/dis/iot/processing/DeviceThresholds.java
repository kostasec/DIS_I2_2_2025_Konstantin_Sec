package rs.ftn.dis.iot.processing;

/**
 * Pragovi po uređaju, deserijalizovani iz odgovora device-registry-service
 * (GET /device/{id}/thresholds).
 */
public record DeviceThresholds(String deviceId, double coThreshold, double tempThreshold) {
}
