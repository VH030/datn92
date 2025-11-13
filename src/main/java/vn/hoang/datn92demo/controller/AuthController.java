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
}
