package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeviceRequestDTO {

    @NotBlank(message = "deviceId không được để trống")
    private String deviceId;

    private String name;

    @NotNull(message = "areaId không được để trống")
    private Long areaId;

    // optional location fields (nullable)
    private Double latitude;
    private Double longitude;

    public DeviceRequestDTO() {}

    public DeviceRequestDTO(String deviceId, String name, Long areaId) {
        this.deviceId = deviceId;
        this.name = name;
        this.areaId = areaId;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
