package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.response.DeviceResponseDTO;
import vn.hoang.datn92demo.dto.response.DeviceSubscriptionResponseDTO;
import vn.hoang.datn92demo.dto.response.DeviceUserResponseDTO;
import vn.hoang.datn92demo.model.Device;
import vn.hoang.datn92demo.model.DeviceSubscription;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.model.WaterLevel;
import vn.hoang.datn92demo.repository.DeviceRepository;
import vn.hoang.datn92demo.repository.DeviceSubscriptionRepository;
import vn.hoang.datn92demo.repository.UserRepository;
import vn.hoang.datn92demo.repository.WaterLevelRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Device Subscriptions", description = "API quản lý đăng ký theo dõi thiết bị & danh sách devices")
public class DeviceSubscriptionController {

    private final DeviceRepository deviceRepository;
    private final DeviceSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final WaterLevelRepository waterLevelRepository;

    public DeviceSubscriptionController(DeviceRepository deviceRepository,
                                        DeviceSubscriptionRepository subscriptionRepository,
                                        UserRepository userRepository,
                                        WaterLevelRepository waterLevelRepository) {
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.waterLevelRepository = waterLevelRepository;
    }

    // ----------------------------
    // User endpoints: subscribe/unsubscribe by deviceId string (ESP32_01)
    // ----------------------------

    /**
     * User đăng ký theo dõi thiết bị bằng deviceId (string, ví dụ "ESP32_01")
     * POST /api/devices/id/{deviceId}/subscribe
     */
    @Operation(summary = "User đăng ký theo dõi thiết bị bằng deviceId (string)")
    @PostMapping("/id/{deviceId}/subscribe")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> subscribeByDeviceIdString(@PathVariable String deviceId, Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị với deviceId: " + deviceId));

        boolean exists = subscriptionRepository.findByUser_Id(user.getId()).stream()
                .anyMatch(s -> s.getDevice() != null && s.getDevice().getId().equals(device.getId()));

        if (!exists) {
            DeviceSubscription sub = new DeviceSubscription();
            sub.setUser(user);
            sub.setDevice(device);
            subscriptionRepository.save(sub);
        }

        return ResponseEntity.ok("Đăng ký theo dõi thiết bị thành công");
    }

    /**
     * User hủy theo dõi thiết bị bằng deviceId (string)
     * DELETE /api/devices/id/{deviceId}/subscribe
     */
    @Operation(summary = "User hủy theo dõi thiết bị bằng deviceId (string)")
    @DeleteMapping("/id/{deviceId}/subscribe")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> unsubscribeByDeviceIdString(@PathVariable String deviceId, Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Tìm device theo deviceId string, rồi xóa subscription theo userId + device.id
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị: " + deviceId));

        // dùng repository method an toàn
        Optional<DeviceSubscription> toDelete = subscriptionRepository.findByUser_IdAndDevice_Id(user.getId(), device.getId());
        toDelete.ifPresent(subscriptionRepository::delete);

        return ResponseEntity.ok("Hủy theo dõi thành công");
    }

    // ----------------------------
    // User endpoints: list user's subscriptions
    // ----------------------------

    /**
     * Lấy danh sách thiết bị mà user đang theo dõi
     * GET /api/devices/subscriptions/me
     * Trả DeviceUserResponseDTO (KHÔNG có id, areaId, createdAt) và isSubscribed = true
     */
    @Operation(summary = "Lấy danh sách thiết bị mà user đang theo dõi")
    @GetMapping("/subscriptions/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<DeviceUserResponseDTO>> mySubscriptions(Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        List<DeviceSubscription> subs = subscriptionRepository.findByUserIdWithDevice(user.getId());
        List<DeviceUserResponseDTO> devices = subs.stream()
                .map(DeviceSubscription::getDevice)
                .filter(d -> d != null)
                .map(d -> {
                    DeviceUserResponseDTO dto = toUserDto(d);
                    dto.setSubscribed(true);
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(devices);
    }

    // ----------------------------
    // New: User endpoint to list all devices (USER)
    // ----------------------------

    /**
     * Lấy danh sách tất cả thiết bị (USER/ADMIN)
     * GET /api/devices
     * Trả DeviceUserResponseDTO cho user view (ẩn id, areaId, createdAt) kèm isSubscribed
     */
    @Operation(summary = "Lấy danh sách tất cả thiết bị (USER)")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<DeviceUserResponseDTO>> listAllDevicesForUser(Authentication authentication) {

        // lấy user hiện tại để đánh dấu isSubscribed
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // danh sách ID các thiết bị user đã subscribe
        List<Long> subscribedIds = getUserSubscribedDeviceIds(user.getId());

        List<Device> devices;
        try {
            devices = deviceRepository.findAllWithArea();
        } catch (Throwable t) {
            devices = deviceRepository.findAll();
        }

        List<DeviceUserResponseDTO> dtos = devices.stream()
                .map(d -> {
                    DeviceUserResponseDTO dto = toUserDto(d);
                    dto.setSubscribed(d.getId() != null && subscribedIds.contains(d.getId()));
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ----------------------------
    // Admin endpoints
    // ----------------------------

    /**
     * Lấy toàn bộ device subscriptions (admin)
     * GET /api/devices/admin/subscriptions
     */
    @Operation(summary = "Lấy toàn bộ device subscriptions (admin)")
    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceSubscriptionResponseDTO>> getAllSubscriptions() {
        List<DeviceSubscription> all = subscriptionRepository.findAllWithDeviceAndUser();
        List<DeviceSubscriptionResponseDTO> list = all.stream().map(s -> {
            var d = s.getDevice();
            var u = s.getUser();
            Long deviceId = d != null ? d.getId() : null;
            String deviceIdentifier = d != null ? d.getDeviceId() : null; // string label
            Long userId = u != null ? u.getId() : null;
            String userPhone = u != null ? u.getPhone() : null;
            return new DeviceSubscriptionResponseDTO(s.getId(), deviceId, deviceIdentifier, userId, userPhone);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy danh sách subscribers của 1 device (admin) theo deviceId string
     * GET /api/devices/admin/{deviceId}/subscribers
     */
    @Operation(summary = "Lấy danh sách subscribers của 1 device (admin) theo deviceId string")
    @GetMapping("/admin/{deviceId}/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSubscribersByDevice(@PathVariable @NotNull String deviceId) {

        // 1) Kiểm tra deviceId string có tồn tại trong bảng devices hay không
        var deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body("Không tìm thấy thiết bị với deviceId: " + deviceId);
        }

        // 2) Lấy danh sách subscription bằng deviceId string
        List<DeviceSubscription> list =
                subscriptionRepository.findByDeviceDeviceIdWithUserAndDevice(deviceId);

        // 3) Convert DTO
        List<DeviceSubscriptionResponseDTO> res = list.stream().map(s -> {
            var d = s.getDevice();
            var u = s.getUser();
            Long devicePk = d != null ? d.getId() : null;
            String deviceIdentifier = d != null ? d.getDeviceId() : null;
            Long userId = u != null ? u.getId() : null;
            String userPhone = u != null ? u.getPhone() : null;

            return new DeviceSubscriptionResponseDTO(
                    s.getId(),
                    devicePk,
                    deviceIdentifier,
                    userId,
                    userPhone
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Xoá subscription (admin)")
    @DeleteMapping("/admin/subscriptions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSubscription(@PathVariable Long id) {
        if (!subscriptionRepository.existsById(id)) return ResponseEntity.notFound().build();
        subscriptionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private DeviceResponseDTO toDtoWithLatest(Device d) {
        DeviceResponseDTO dto = new DeviceResponseDTO();
        dto.setId(d.getId());
        dto.setDeviceId(d.getDeviceId());
        dto.setName(d.getName());

        if (d.getArea() != null) {
            try {
                dto.setAreaId(d.getArea().getId());
                dto.setAreaName(d.getArea().getName());
            } catch (Exception ex) {
                // defensive: proxy may throw; ignore if happens
            }
        }

        dto.setCreatedAt(d.getCreatedAt());
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());

        // latest water level (nullable)
        try {
            Optional<WaterLevel> latest = waterLevelRepository.findTopByDeviceIdOrderByTimestampDesc(d.getDeviceId());
            dto.setLastWaterLevel(latest.map(WaterLevel::getLevel).orElse(null));
        } catch (Throwable t) {
            dto.setLastWaterLevel(null);
        }

        return dto;
    }

    private DeviceUserResponseDTO toUserDto(Device d) {
        DeviceUserResponseDTO dto = new DeviceUserResponseDTO();
        dto.setDeviceId(d.getDeviceId());
        dto.setName(d.getName());

        // --- Area Name (SAFE) ---
        String areaName = null;
        try {
            if (d.getArea() != null) {
                areaName = d.getArea().getName();
            }
        } catch (Exception ignored) { }
        dto.setAreaName(areaName);

        // --- Coordinates ---
        dto.setLatitude(d.getLatitude());
        dto.setLongitude(d.getLongitude());

        // --- Last water level ---
        try {
            dto.setLastWaterLevel(
                    waterLevelRepository
                            .findTopByDeviceIdOrderByTimestampDesc(d.getDeviceId())
                            .map(WaterLevel::getLevel)
                            .orElse(null)
            );
        } catch (Exception e) {
            dto.setLastWaterLevel(null);
        }

        dto.setSubscribed(false);
        return dto;
    }

    private List<Long> getUserSubscribedDeviceIds(Long userId) {
        return subscriptionRepository.findByUser_Id(userId).stream()
                .filter(s -> s.getDevice() != null && s.getDevice().getId() != null)
                .map(s -> s.getDevice().getId())
                .collect(Collectors.toList());
    }
}
