package rs.ftn.dis.iot.deviceregistry;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "devices")
public class DeviceEntity {

    @Id
    private String deviceId;
    private String name;
    private String location;
    private String type;

    // Pragovi za alarmiranje, specifični po uređaju (sync ka processing-service).
    private Double coThreshold;
    private Double tempThreshold;

    public DeviceEntity() {}

    public DeviceEntity(String deviceId, String name, String location, String type) {
        this.deviceId = deviceId;
        this.name = name;
        this.location = location;
        this.type = type;
    }

    public DeviceEntity(String deviceId, String name, String location, String type,
                        Double coThreshold, Double tempThreshold) {
        this(deviceId, name, location, type);
        this.coThreshold = coThreshold;
        this.tempThreshold = tempThreshold;
    }

    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public Double getCoThreshold() { return coThreshold; }
    public Double getTempThreshold() { return tempThreshold; }

    // Setteri su neophodni da Jackson moze da deserijalizuje telo POST /device zahteva.
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setType(String type) { this.type = type; }
    public void setCoThreshold(Double coThreshold) { this.coThreshold = coThreshold; }
    public void setTempThreshold(Double tempThreshold) { this.tempThreshold = tempThreshold; }
}
