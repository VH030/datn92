package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoang.datn92demo.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
