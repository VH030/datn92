package vn.hoang.datn92demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.hoang.datn92demo.dto.request.UserRegisterRequestDTO;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public User register(UserRegisterRequestDTO dto) {
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        return userRepository.save(user);
    }
}

