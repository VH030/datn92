package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Date;

public class WaterLevelRequestDTO {

    @NotNull(message = "Mực nước không được để trống")
    private Double level;

    @NotNull(message = "Device ID không được để trống")
    private String deviceId;

    private Date timestamp = new Date();

    // NEW: loại cảnh báo từ ESP32 gửi lên
    // NORMAL | WARNING | ALARM
    private String alertType;

    public Double getLevel() {
        return level;
    }

    public void setLevel(Double level) {
        this.level = level;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
}
