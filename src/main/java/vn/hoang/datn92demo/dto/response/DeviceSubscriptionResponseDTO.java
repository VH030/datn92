package vn.hoang.datn92demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceSubscriptionResponseDTO {

    private Long subscriptionId;

    // id PK của bảng devices
    private Long deviceId;

    // deviceId string từ ESP32 (“ESP32-01”)
    private String deviceIdString;

    private Long userId;
    private String userPhone;
}
