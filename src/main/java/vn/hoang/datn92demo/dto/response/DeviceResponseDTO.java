package vn.hoang.datn92demo.dto.response;

import java.util.Date;

public class DeviceResponseDTO {

    private Long id;
    private String deviceId;
    private String name;
    private Long areaId;
    private String areaName;
    private Date createdAt;

    // NEW
    private Double latitude;
    private Double longitude;

    // NEW: latest water level (nullable)
    private Double lastWaterLevel;

    public DeviceResponseDTO() {}

    public DeviceResponseDTO(Long id, String deviceId, String name, Long areaId, String areaName, Date createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.areaId = areaId;
        this.areaName = areaName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLastWaterLevel() { return lastWaterLevel; }
    public void setLastWaterLevel(Double lastWaterLevel) { this.lastWaterLevel = lastWaterLevel; }
}
