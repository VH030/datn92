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
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.service.OtpService;
import vn.hoang.datn92demo.service.UserService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API đăng ký và đăng nhập người dùng (có OTP)")
@CrossOrigin(origins = "*")
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
    public ResponseEntity<String> register(@Valid @RequestBody UserRegisterRequestDTO dto) {
        if (userService.existsByPhone(dto.getPhone())) {
            return ResponseEntity.badRequest().body("Số điện thoại đã tồn tại!");
        }
        otpService.sendOtp(dto.getPhone());
        return ResponseEntity.ok("Đã gửi OTP đến số điện thoại: " + dto.getPhone());
    }

    @Operation(summary = "Xác minh OTP để hoàn tất đăng ký")
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String phone,
                                            @RequestParam String otp,
                                            @Valid @RequestBody UserRegisterRequestDTO dto) {
        if (otpService.verifyOtp(phone, otp)) {
            User user = userService.register(dto);
            return ResponseEntity.ok("Đăng ký thành công cho số: " + user.getPhone());
        }
        return ResponseEntity.badRequest().body("OTP không hợp lệ hoặc đã hết hạn!");
    }


    @Operation(summary = "Đăng nhập người dùng (nhận JWT Token)")
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getPhone(), dto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(dto.getPhone());
        return ResponseEntity.ok(token);
    }
}
