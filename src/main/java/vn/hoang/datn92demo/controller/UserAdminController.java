package vn.hoang.datn92demo.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.UserAdminRequestDTO;
import vn.hoang.datn92demo.dto.response.UserAdminResponseDTO;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    // Danh sách tất cả user (không hiển thị mật khẩu)
    @GetMapping
    public List<UserAdminResponseDTO> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserAdminResponseDTO::new)
                .toList();
    }

    // Xem thông tin 1 user
    @GetMapping("/{id}")
    public ResponseEntity<UserAdminResponseDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserAdminResponseDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cập nhật thông tin user (admin không đổi được mật khẩu user)
    @PutMapping("/{id}")
    public ResponseEntity<UserAdminResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserAdminRequestDTO dto) {
        try {
            User updatedUser = userService.updateUserAsAdmin(id, dto);
            return ResponseEntity.ok(new UserAdminResponseDTO(updatedUser));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Đã xóa user");
    }
}
