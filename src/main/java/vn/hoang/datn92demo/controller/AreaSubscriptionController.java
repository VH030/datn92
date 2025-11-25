package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.response.AreaResponseDTO;
import vn.hoang.datn92demo.dto.response.AreaSubscriptionResponseDTO;
import vn.hoang.datn92demo.model.Area;
import vn.hoang.datn92demo.model.AreaSubscription;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.repository.AreaRepository;
import vn.hoang.datn92demo.repository.AreaSubscriptionRepository;
import vn.hoang.datn92demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/areas")
@Tag(name = "Area Subscriptions", description = "API quản lý đăng ký theo dõi khu vực")
public class AreaSubscriptionController {

    private final AreaRepository areaRepository;
    private final AreaSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public AreaSubscriptionController(AreaRepository areaRepository,
                                      AreaSubscriptionRepository subscriptionRepository,
                                      UserRepository userRepository) {
        this.areaRepository = areaRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    // ----------------------------
    // User endpoints
    // ----------------------------

    /**
     * User đăng ký theo dõi 1 khu vực
     * POST /api/areas/{areaId}/subscribe
     */
    @Operation(summary = "User đăng ký theo dõi khu vực")
    @PostMapping("/{areaId}/subscribe")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> subscribe(@PathVariable @NotNull Long areaId, Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));

        boolean exists = subscriptionRepository.findByUser_Id(user.getId()).stream()
                .anyMatch(s -> s.getArea() != null && s.getArea().getId().equals(areaId));
        if (!exists) {
            AreaSubscription s = new AreaSubscription();
            s.setUser(user);
            s.setArea(area);
            subscriptionRepository.save(s);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * User hủy theo dõi 1 khu vực
     * DELETE /api/areas/{areaId}/subscribe
     */
    @Operation(summary = "User hủy theo dõi khu vực")
    @DeleteMapping("/{areaId}/subscribe")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> unsubscribe(@PathVariable @NotNull Long areaId, Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        subscriptionRepository.findByUser_Id(user.getId()).stream()
                .filter(s -> s.getArea() != null && s.getArea().getId().equals(areaId))
                .findFirst()
                .ifPresent(subscriptionRepository::delete);

        return ResponseEntity.ok().build();
    }

    /**
     * Lấy danh sách khu vực mà user đang theo dõi
     * GET /api/areas/subscriptions/me
     */
    @Operation(summary = "Lấy danh sách khu vực mà user đang theo dõi")
    @GetMapping("/subscriptions/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<AreaResponseDTO>> mySubscriptions(Authentication authentication) {
        String phone = authentication.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Sử dụng fetch-join method để tránh lazy init nếu cần (repository method exists)
        List<AreaSubscription> subs = subscriptionRepository.findByUserIdWithArea(user.getId());
        List<AreaResponseDTO> areas = subs.stream()
                .map(AreaSubscription::getArea)
                .filter(a -> a != null)
                .map(a -> new AreaResponseDTO(a.getId(), a.getCode(), a.getName(), a.getDescription()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(areas);
    }

    // ----------------------------
    // Admin endpoints
    // ----------------------------

    /**
     * Lấy toàn bộ subscriptions (admin)
     * GET /api/areas/admin/subscriptions
     */
    @Operation(summary = "Lấy toàn bộ subscriptions (admin)")
    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AreaSubscriptionResponseDTO>> getAllSubscriptions() {
        List<AreaSubscription> all = subscriptionRepository.findAllWithAreaAndUser();
        List<AreaSubscriptionResponseDTO> list = all.stream()
                .map(this::toSubscriptionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Lấy danh sách subscribers của 1 khu vực (admin)
     * GET /api/areas/admin/{areaId}/subscribers
     */
    @Operation(summary = "Lấy danh sách subscribers của 1 khu vực (admin)")
    @GetMapping("/admin/{areaId}/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AreaSubscriptionResponseDTO>> getSubscribersByArea(@PathVariable @NotNull Long areaId) {
        List<AreaSubscription> list = subscriptionRepository.findByAreaIdWithUserAndArea(areaId);
        List<AreaSubscriptionResponseDTO> res = list.stream().map(this::toSubscriptionDto).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    /**
     * Admin xóa 1 subscription theo id
     * DELETE /api/areas/admin/subscriptions/{id}
     */
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
    private AreaSubscriptionResponseDTO toSubscriptionDto(AreaSubscription s) {
        Long subscriptionId = s.getId();
        Long areaId = s.getArea() != null ? s.getArea().getId() : null;
        String areaName = s.getArea() != null ? s.getArea().getName() : null;
        Long userId = s.getUser() != null ? s.getUser().getId() : null;
        String userPhone = s.getUser() != null ? s.getUser().getPhone() : null;
        return new AreaSubscriptionResponseDTO(subscriptionId, areaId, areaName, userId, userPhone);
    }
}
