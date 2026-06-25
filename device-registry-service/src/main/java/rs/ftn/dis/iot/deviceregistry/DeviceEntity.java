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

    public DeviceEntity() {}

    public DeviceEntity(String deviceId, String name, String location, String type) {
        this.deviceId = deviceId;
        this.name = name;
        this.location = location;
        this.type = type;
    }

    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getType() { return type; }
}