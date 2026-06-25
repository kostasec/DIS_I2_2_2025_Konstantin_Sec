package rs.ftn.dis.iot.deviceregistry;

public class Device {
    private String deviceId;
    private String name;
    private String location;
    private String type;

    public Device(String deviceId, String name, String location, String type) {
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
