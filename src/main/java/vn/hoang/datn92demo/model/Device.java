package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    // NEW: optional location fields
    private Double latitude;
    private Double longitude;

    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
