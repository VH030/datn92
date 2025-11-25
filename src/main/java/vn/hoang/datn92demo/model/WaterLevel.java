package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "water_levels")
public class WaterLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double level;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    // NEW: deviceId để biết bản ghi thuộc thiết bị nào
    @Column(name = "device_id")
    private String deviceId;


    //NEW: alert type
    @Column(name = "alert_type")
    private String alertType;

    public WaterLevel() {
        this.timestamp = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLevel() {
        return level;
    }

    public void setLevel(Double level) {
        this.level = level;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAlertType() { return alertType; }

    public void setAlertType(String alertType) { this.alertType = alertType; }

}
