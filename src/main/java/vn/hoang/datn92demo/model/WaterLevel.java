package vn.hoang.datn92demo.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "water_levels")
public class WaterLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double level;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public WaterLevel() {
        this.timestamp = new Date(); // Gán mặc định thời gian hiện tại khi khởi tạo
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
}

