package vn.hoang.datn92demo.dto.response;

public class DeviceUserResponseDTO {

    private String deviceId;
    private String name;
    private String areaName;

    private Double latitude;
    private Double longitude;

    private Double lastWaterLevel;

    // NEW
    private boolean isSubscribed;

    public DeviceUserResponseDTO() {}

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLastWaterLevel() { return lastWaterLevel; }
    public void setLastWaterLevel(Double lastWaterLevel) { this.lastWaterLevel = lastWaterLevel; }

    public boolean isSubscribed() { return isSubscribed; }
    public void setSubscribed(boolean subscribed) { isSubscribed = subscribed; }
}
