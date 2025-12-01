package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoang.datn92demo.model.DeviceSubscription;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DeviceSubscriptionRepository extends JpaRepository<DeviceSubscription, Long> {

    // Lấy subscription theo deviceId (numeric PK)
    List<DeviceSubscription> findByDevice_Id(Long deviceId);

    // Lấy subscription theo userId
    List<DeviceSubscription> findByUser_Id(Long userId);

    // TÌM 1 subscription bằng userId + device.id (dùng để xóa an toàn)
    Optional<DeviceSubscription> findByUser_IdAndDevice_Id(Long userId, Long deviceId);

    // XÓA trực tiếp bằng userId + device.id (một SQL) — tiện và hiệu quả
    @Modifying
    @Transactional
    @Query("DELETE FROM DeviceSubscription s WHERE s.user.id = :userId AND s.device.id = :deviceId")
    void deleteByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") Long deviceId);

    // -----------------------------
    // Fetch join (fetch device + area + user) để tránh lazy init của area
    // -----------------------------

    @Query("SELECT s FROM DeviceSubscription s " +
            "LEFT JOIN FETCH s.device d " +
            "LEFT JOIN FETCH d.area a " +
            "LEFT JOIN FETCH s.user u")
    List<DeviceSubscription> findAllWithDeviceAndUser();

    @Query("SELECT s FROM DeviceSubscription s " +
            "LEFT JOIN FETCH s.device d " +
            "LEFT JOIN FETCH d.area a " +
            "LEFT JOIN FETCH s.user u " +
            "WHERE d.id = :deviceId")
    List<DeviceSubscription> findByDeviceIdWithUserAndDevice(@Param("deviceId") Long deviceId);

    @Query("SELECT s FROM DeviceSubscription s " +
            "LEFT JOIN FETCH s.device d " +
            "LEFT JOIN FETCH d.area a " +
            "LEFT JOIN FETCH s.user u " +
            "WHERE u.id = :userId")
    List<DeviceSubscription> findByUserIdWithDevice(@Param("userId") Long userId);

    // -------------------------------------------
    //  METHOD QUAN TRỌNG CHO WATERLEVELSERVICE
    // Tìm theo deviceId dạng String (ESP32-01) — cũng fetch area
    // -------------------------------------------
    @Query("SELECT s FROM DeviceSubscription s " +
            "LEFT JOIN FETCH s.device d " +
            "LEFT JOIN FETCH d.area a " +
            "LEFT JOIN FETCH s.user u " +
            "WHERE d.deviceId = :deviceDeviceId")
    List<DeviceSubscription> findByDeviceDeviceIdWithUserAndDevice(@Param("deviceDeviceId") String deviceDeviceId);
}
