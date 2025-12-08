package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.time.Instant;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người nhận thông báo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Thiết bị liên quan (có thể null nếu là thông báo hệ thống)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    // Loại thông báo tổng quát: WATER_ALERT, OTP, SYSTEM...
    @Column(length = 50)
    private String type;

    // alertType chi tiết: WARNING_ENTER, ALARM_UP, NORMAL_PERIODIC...
    @Column(name = "alert_type", length = 50)
    private String alertType;

    // Giá trị mực nước (nếu có)
    @Column(name = "level_value")
    private Double levelValue;

    // Nội dung hiển thị cho user
    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    // ===== getters & setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Double getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(Double levelValue) {
        this.levelValue = levelValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
