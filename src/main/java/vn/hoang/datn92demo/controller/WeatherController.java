package vn.hoang.datn92demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.response.WeatherResponseDTO;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.repository.DeviceRepository;
import vn.hoang.datn92demo.service.WeatherService;

import java.util.Optional;

/**
 * WeatherController - user chỉ cần nhập deviceDeviceId để xem thời tiết.
 * Không kiểm tra subscription.
 *
 * Endpoint mới:
 * GET /api/devices/{deviceDeviceId}/weather
 */
@RestController
@RequestMapping("/api")
public class WeatherController {

    private final DeviceRepository deviceRepository;
    private final WeatherService weatherService;

    public WeatherController(DeviceRepository deviceRepository,
                             WeatherService weatherService) {
        this.deviceRepository = deviceRepository;
        this.weatherService = weatherService;
    }

    /**
     * Ví dụ URL:
     * GET /api/devices/ESP32-01/weather
     */
    @GetMapping("/devices/{deviceDeviceId}/weather")
    public ResponseEntity<?> getWeatherByDeviceDeviceId(@PathVariable String deviceDeviceId) {
        if (deviceDeviceId == null || deviceDeviceId.isBlank()) {
            return ResponseEntity.badRequest().body("deviceDeviceId không hợp lệ");
        }

        // 1. Tìm device theo deviceDeviceId (String)
        Optional<Device> optDevice = deviceRepository.findByDeviceId(deviceDeviceId);
        if (optDevice.isEmpty()) {
            return ResponseEntity.status(404).body("Không tìm thấy thiết bị với deviceId: " + deviceDeviceId);
        }
        Device device = optDevice.get();

        // 2. Kiểm tra lat/lon
        Double lat = device.getLatitude();
        Double lon = device.getLongitude();
        if (lat == null || lon == null) {
            return ResponseEntity.badRequest().body("Thiết bị chưa có vị trí (latitude/longitude)");
        }

        // 3. Gọi WeatherService
        WeatherResponseDTO weather = weatherService.getWeatherByLatLon(lat, lon);
        if (weather == null) {
            return ResponseEntity.status(502).body("Không lấy được dữ liệu thời tiết từ provider");
        }

        return ResponseEntity.ok(weather);
    }
}
