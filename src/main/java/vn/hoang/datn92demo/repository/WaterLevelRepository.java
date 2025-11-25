package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoang.datn92demo.model.WaterLevel;

import java.util.Optional;

@Repository
public interface WaterLevelRepository extends JpaRepository<WaterLevel, Long> {
    Optional<WaterLevel> findTopByDeviceIdOrderByTimestampDesc(String deviceId);
}

