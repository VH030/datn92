package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoang.datn92demo.model.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    // Tìm theo deviceId (ESP32 gửi)
    Optional<Device> findByDeviceId(String deviceId);

    // Lấy tất cả devices và fetch luôn area để tránh lỗi LazyInitializationException
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.area")
    List<Device> findAllWithArea();

    // Lấy devices theo areaId có fetch join
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.area WHERE d.area.id = :areaId")
    List<Device> findByAreaIdWithArea(@Param("areaId") Long areaId);

    // Tùy chọn: query mặc định của JPA (không fetch join)
    List<Device> findByArea_Id(Long areaId);

}
