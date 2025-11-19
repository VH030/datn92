package vn.hoang.datn92demo.controller.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.dto.request.ChangePasswordRequestDTO;
import vn.hoang.datn92demo.dto.request.UserUpdateRequestDTO;
import vn.hoang.datn92demo.dto.response.UserAdminResponseDTO;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy profile của chính user (dựa trên phone từ token)
     */
    @GetMapping("/me")
    public ResponseEntity<UserAdminResponseDTO> getMyProfile(Authentication authentication) {
        String phone = authentication.getName();   // Token đang lưu phone
        User user = userService.findByPhone(phone);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new UserAdminResponseDTO(user));
    }

    /**
     * Cập nhật thông tin user
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(Authentication authentication,
                                             @Valid @RequestBody UserUpdateRequestDTO dto) {

        String phone = authentication.getName(); // Token lưu phone
        User user = userService.findByPhone(phone);

        if (user == null) {
            return ResponseEntity.status(404).body("Không tìm thấy user");
        }

        try {
            User updated = userService.updateProfile(user.getId(), dto);
            return ResponseEntity.ok(new UserAdminResponseDTO(updated));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Đổi mật khẩu user
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication authentication,
                                            @Valid @RequestBody ChangePasswordRequestDTO dto) {

        String phone = authentication.getName(); // Token lưu phone
        User user = userService.findByPhone(phone);

        if (user == null) {
            return ResponseEntity.status(404).body("Không tìm thấy user");
        }

        try {
            userService.changePassword(user.getId(), dto);
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
