package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.WaterLevelRequestDTO;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.service.WaterLevelService;

import java.util.List;

@RestController
@RequestMapping("/api/water-levels")
@Tag(name = "Water Level", description = "API giám sát mực nước")
@CrossOrigin(origins = "*")
public class WaterLevelController {

    private final WaterLevelService service;

    @Value("${app.device.api-key:MY_SECRET_DEVICE_KEY}")
    private String deviceApiKey;

    public WaterLevelController(WaterLevelService service) {
        this.service = service;
    }

    @Operation(summary = "Lấy danh sách mực nước hiện tại")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<WaterLevel> getAll(Authentication authentication) {
        // Nếu cần, có thể lấy phone của user: authentication.getName()
        return service.getAll();
    }

    @Operation(summary = "Thêm dữ liệu mực nước mới (admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WaterLevel addLevel(@Valid @RequestBody WaterLevelRequestDTO dto) {
        return service.save(dto);
    }

    /**
     * Endpoint dành cho thiết bị (ESP32...) gửi dữ liệu bằng API key.
     * ESP32 gọi POST /api/water-levels/device với header X-API-KEY.
     * Đây là endpoint public (SecurityConfig đã cho phép /api/water-levels/device).
     */
    @Operation(summary = "Thiết bị gửi mực nước (dùng API KEY)")
    @PostMapping("/device")
    public WaterLevel addFromDevice(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @Valid @RequestBody WaterLevelRequestDTO dto
    ) {
        if (apiKey == null || !apiKey.equals(deviceApiKey)) {
            throw new RuntimeException("Invalid or missing API key");
        }

        return service.save(dto);
    }
}
