package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoang.datn92demo.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}

