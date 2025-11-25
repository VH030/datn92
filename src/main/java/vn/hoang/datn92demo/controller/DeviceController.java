package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.DeviceRequestDTO;
import vn.hoang.datn92demo.dto.response.DeviceResponseDTO;
import vn.hoang.datn92demo.model.Area;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.AreaRepository;
import vn.hoang.datn92demo.repository.DeviceRepository;
import vn.hoang.datn92demo.repository.WaterLevelRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/devices")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final AreaRepository areaRepository;
    private final WaterLevelRepository waterLevelRepository;

    public DeviceController(DeviceRepository deviceRepository,
                            AreaRepository areaRepository,
                            WaterLevelRepository waterLevelRepository) {
        this.deviceRepository = deviceRepository;
        this.areaRepository = areaRepository;
        this.waterLevelRepository = waterLevelRepository;
    }

    @Operation(summary = "Lấy danh sách thiết bị (admin)")
    @GetMapping
    public ResponseEntity<List<DeviceResponseDTO>> getAll() {
        // sử dụng fetch-join nếu repository có (tối ưu) — fallback về findAll()
        List<Device> devices;
        try {
            devices = deviceRepository.findAllWithArea();
        } catch (Throwable t) {
            devices = deviceRepository.findAll();
        }

        List<DeviceResponseDTO> dtos = devices.stream()
                .map(this::toDtoWithLatest)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Lấy thiết bị theo id (admin)")
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDTO> getById(@PathVariable Long id) {
        return deviceRepository.findById(id)
                .map(d -> ResponseEntity.ok(toDtoWithLatest(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Tạo thiết bị (admin)")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DeviceRequestDTO req) {
        if (req.getDeviceId() == null || req.getDeviceId().isBlank()) {
            return ResponseEntity.badRequest().body("deviceId is required");
        }
        if (deviceRepository.findByDeviceId(req.getDeviceId()).isPresent()) {
            return ResponseEntity.badRequest().body("deviceId already exists");
        }

        Optional<Area> maybeArea = areaRepository.findById(req.getAreaId());
        if (maybeArea.isEmpty()) {
            return ResponseEntity.badRequest().body("Area not found");
        }

        Device d = new Device();
        d.setDeviceId(req.getDeviceId());
        d.setName(req.getName());
        d.setArea(maybeArea.get());

        // optional location
        if (req.getLatitude() != null) d.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) d.setLongitude(req.getLongitude());

        Device saved = deviceRepository.save(d);

        return ResponseEntity.ok(toDtoWithLatest(saved));
    }

    @Operation(summary = "Cập nhật thiết bị (admin)")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody DeviceRequestDTO req) {
        Optional<Device> opt = deviceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Device d = opt.get();

        if (req.getName() != null) d.setName(req.getName());

        if (req.getDeviceId() != null && !req.getDeviceId().equals(d.getDeviceId())) {
            if (deviceRepository.findByDeviceId(req.getDeviceId()).isPresent()) {
                return ResponseEntity.badRequest().body("deviceId already exists");
            }
            d.setDeviceId(req.getDeviceId());
        }

        if (req.getAreaId() != null && (d.getArea() == null || !req.getAreaId().equals(d.getArea().getId()))) {
            Optional<Area> maybeArea = areaRepository.findById(req.getAreaId());
            if (maybeArea.isEmpty()) {
                return ResponseEntity.badRequest().body("Area not found");
            }
            d.setArea(maybeArea.get());
        }

        // optional update location fields
        if (req.getLatitude() != null) d.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) d.setLongitude(req.getLongitude());

        Device saved = deviceRepository.save(d);
        return ResponseEntity.ok(toDtoWithLatest(saved));
    }

    @Operation(summary = "Cập nhật vị trí thiết bị (admin)")
    @PatchMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        Double lat = body.get("latitude");
        Double lon = body.get("longitude");

        if (lat == null || lon == null) {
            return ResponseEntity.badRequest().body("latitude and longitude required");
        }
        // basic validation
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ResponseEntity.badRequest().body("latitude or longitude out of range");
        }

        Optional<Device> opt = deviceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Device device = opt.get();
        device.setLatitude(lat);
        device.setLongitude(lon);
        deviceRepository.save(device);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Xóa thiết bị (admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!deviceRepository.existsById(id)) return ResponseEntity.notFound().build();
        deviceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // -----------------------
    // Helpers
    // -----------------------
    private DeviceResponseDTO toDtoWithLatest(Device d) {
        Long areaId = null;
        String areaName = null;

        if (d.getArea() != null) {
            try {
                Long maybeAreaId = d.getArea().getId();
                if (maybeAreaId != null) {
                    Optional<Area> aOpt = areaRepository.findById(maybeAreaId);
                    if (aOpt.isPresent()) {
                        areaId = aOpt.get().getId();
                        areaName = aOpt.get().getName();
                    } else {
                        areaId = maybeAreaId;
                    }
                }
            } catch (Exception ex) {
                // defensive: if proxy fails, do nothing (areaName stays null)
            }
        }

        DeviceResponseDTO dto = new DeviceResponseDTO();
        dto.setId(d.getId());
        dto.setDeviceId(d.getDeviceId());
        dto.setName(d.getName());
        dto.setAreaId(areaId);
        dto.setAreaName(areaName);
        dto.setCreatedAt(d.getCreatedAt());

        // location
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());

        // latest water level (optional)
        try {
            Optional<WaterLevel> latest = waterLevelRepository.findTopByDeviceIdOrderByTimestampDesc(d.getDeviceId());
            dto.setLastWaterLevel(latest.map(WaterLevel::getLevel).orElse(null));
        } catch (Throwable t) {
            // If repository method missing or fails, just ignore (client gets null)
            dto.setLastWaterLevel(null);
        }

        return dto;
    }
}
