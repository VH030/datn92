package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Date;

public class WaterLevelRequestDTO {

    @NotNull(message = "Mực nước không được để trống")
    private Double level;

    private Date timestamp = new Date();

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

