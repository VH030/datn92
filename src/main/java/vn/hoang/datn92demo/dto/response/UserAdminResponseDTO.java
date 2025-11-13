package vn.hoang.datn92demo.dto.response;

import lombok.Data;
import vn.hoang.datn92demo.model.User;

@Data
public class UserAdminResponseDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private User.Role role;

    public UserAdminResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}
