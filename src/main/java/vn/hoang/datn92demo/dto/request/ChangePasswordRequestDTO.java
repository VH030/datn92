package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;
}
