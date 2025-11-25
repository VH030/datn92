package vn.hoang.datn92demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.hoang.datn92demo.dto.request.UserAdminRequestDTO;
import vn.hoang.datn92demo.dto.request.UserRegisterRequestDTO;
import vn.hoang.datn92demo.dto.request.UserUpdateRequestDTO;
import vn.hoang.datn92demo.dto.request.ChangePasswordRequestDTO;
import vn.hoang.datn92demo.exception.ResourceNotFoundException;
import vn.hoang.datn92demo.model.User;
import vn.hoang.datn92demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- ĐĂNG KÝ / TÌM KIẾM ----------
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        // Assumes UserRepository has existsByUsername
        return userRepository.existsByUsername(username);
    }

    public Optional<User> findByUsername(String username) {
        // Assumes UserRepository has findByUsername
        return userRepository.findByUsername(username);
    }

    public User register(UserRegisterRequestDTO dto) {
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.USER);

        return userRepository.save(user);
    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    // ---------- QUẢN LÝ USER (ADMIN) ----------
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Chỉnh sửa thông tin user
     */
    public User updateUserAsAdmin(Long id, UserAdminRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        return userRepository.save(user);
    }


    //delete user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy user id " + id);
        }
        userRepository.deleteById(id);
    }

    // ---------- CHỨC NĂNG CHO USER TỰ QUẢN LÝ ----------
    /**
     * Cập nhật profile của chính user (USER không thể đổi role).
     * Kiểm tra username/phone/email không trùng với user khác.
     *
     * @param userId id của user đang cập nhật
     * @param dto    dữ liệu cập nhật (không chứa role, không chứa password)
     * @return user đã được cập nhật
     */
    public User updateProfile(Long userId, UserUpdateRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // kiểm tra trùng username (ngoại trừ chính họ)
        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã được sử dụng bởi người khác");
        }

        // kiểm tra trùng phone (ngoại trừ chính họ)
        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng bởi người khác");
        }

        // kiểm tra trùng email (ngoại trừ chính họ)
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng bởi người khác");
        }

        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());

        // KHÔNG cập nhật role, KHÔNG cập nhật password tại đây
        return userRepository.save(user);
    }

    /**
     * Đổi mật khẩu yêu cầu nhập mật khẩu cũ.
     *
     * @param userId id của user đang đổi mật khẩu
     * @param dto    chứa oldPassword và newPassword
     */
    public void changePassword(Long userId, ChangePasswordRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }

        // lưu mật khẩu mới đã mã hóa
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }
}
