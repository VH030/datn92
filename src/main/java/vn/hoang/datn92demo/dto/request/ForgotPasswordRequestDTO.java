package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequestDTO {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    public ForgotPasswordRequestDTO() {}

    public ForgotPasswordRequestDTO(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
