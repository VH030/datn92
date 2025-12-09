package vn.hoang.datn92demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordVerifyDTO {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "OTP không được để trống")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;

    public ForgotPasswordVerifyDTO() {}

    public ForgotPasswordVerifyDTO(String phone, String otp, String newPassword) {
        this.phone = phone;
        this.otp = otp;
        this.newPassword = newPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
