package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequestDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @Email
    private String email;
}
