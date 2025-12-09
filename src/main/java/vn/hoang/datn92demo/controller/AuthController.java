package vn.hoang.datn92demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.hoang.datn92demo.config.JwtTokenProvider;
import vn.hoang.datn92demo.dto.request.UserLoginRequestDTO;
import vn.hoang.datn92demo.dto.request.UserRegisterRequestDTO;
import vn.hoang.datn92demo.dto.request.VerifyOtpRequestDTO;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.service.OtpService;
import vn.hoang.datn92demo.service.UserService;
import vn.hoang.datn92demo.dto.request.ForgotPasswordRequestDTO;
import vn.hoang.datn92demo.dto.request.ForgotPasswordVerifyDTO;
import vn.hoang.datn92demo.exception.ResourceNotFoundException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API đăng ký và đăng nhập người dùng (có OTP + phân quyền)")
@CrossOrigin(origins = "http://localhost:9000")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          OtpService otpService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.otpService = otpService;
    }

    @Operation(summary = "Gửi OTP xác thực khi đăng ký tài khoản")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterRequestDTO dto) {
        if (userService.existsByPhone(dto.getPhone())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Số điện thoại đã tồn tại!"
            ));
        }
        otpService.sendOtp(dto.getPhone());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi OTP đến số điện thoại: " + dto.getPhone()
        ));
    }

    @Operation(summary = "Xác minh OTP để hoàn tất đăng ký (body JSON)")
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO dto) {
        // kiểm tra OTP
        if (!otpService.verifyOtp(dto.getPhone(), dto.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "OTP không hợp lệ hoặc đã hết hạn!"
            ));
        }

        // tạo UserRegisterRequestDTO từ VerifyOtpRequestDTO (tái sử dụng logic register)
        UserRegisterRequestDTO regDto = new UserRegisterRequestDTO();
        regDto.setUsername(dto.getUsername());
        regDto.setFullName(dto.getFullName());
        regDto.setEmail(dto.getEmail());
        regDto.setPhone(dto.getPhone());
        regDto.setPassword(dto.getPassword());

        User user = userService.register(regDto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng ký thành công!",
                "user", Map.of(
                        "id", user.getId(),
                        "fullname", user.getFullName(),
                        "phone", user.getPhone(),
                        "role", user.getRole().name()
                )
        ));
    }

    @Operation(summary = "Đăng nhập người dùng")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequestDTO dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userService.findByPhone(dto.getPhone());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy người dùng!"
                ));
            }

            String token = jwtTokenProvider.generateToken(user.getPhone(), user.getRole().name());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token,
                    "role", user.getRole().name(),
                    "user", Map.of(
                            "id", user.getId(),
                            "fullname", user.getFullName(),
                            "phone", user.getPhone(),
                            "role", user.getRole().name()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Sai số điện thoại hoặc mật khẩu!"
            ));
        }
    }

    @Operation(summary = "Gửi OTP khi quên mật khẩu")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        if (dto == null || dto.getPhone() == null || dto.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Số điện thoại không hợp lệ!"
            ));
        }

        // Giữ privacy: trả same response dù user có tồn tại hay không
        if (userService.existsByPhone(dto.getPhone())) {
            try {
                otpService.sendOtp(dto.getPhone());
            } catch (Exception e) {
                // logger.error("Failed to send OTP for forgot-password", e);
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "message", "Không thể gửi OTP, vui lòng thử lại sau."
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nếu số điện thoại tồn tại trong hệ thống, mã OTP đã được gửi."
        ));
    }

    @Operation(summary = "Xác thực OTP quên mật khẩu và đổi mật khẩu mới")
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyForgotPassword(@Valid @RequestBody ForgotPasswordVerifyDTO dto) {
        if (dto == null
                || dto.getPhone() == null || dto.getPhone().isBlank()
                || dto.getOtp() == null || dto.getOtp().isBlank()
                || dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Tham số không hợp lệ!"
            ));
        }

        boolean ok;
        try {
            ok = otpService.verifyOtp(dto.getPhone(), dto.getOtp());
        } catch (Exception ex) {
            ok = false;
        }

        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "OTP không hợp lệ hoặc đã hết hạn!"
            ));
        }

        try {
            // Dùng method mới trong UserService để reset password
            userService.resetPasswordByPhone(dto.getPhone(), dto.getNewPassword());
        } catch (ResourceNotFoundException rnfe) {
            // Hiếm khi xảy ra: phone tồn tại khi gửi OTP nhưng user bị xóa sau đó
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Người dùng không tồn tại!"
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lưu mật khẩu mới."
            ));
        }

        // (Tuỳ chọn) invalidate JWT hiện tại nếu bạn có hệ thống blacklist token

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đổi mật khẩu thành công!"
        ));
    }

}
