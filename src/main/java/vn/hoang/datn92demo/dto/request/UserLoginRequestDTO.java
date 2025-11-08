package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UserLoginRequestDTO {
    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

