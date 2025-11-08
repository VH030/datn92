package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoang.datn92demo.model.WaterLevel;

@Repository
public interface WaterLevelRepository extends JpaRepository<WaterLevel, Long> {
}

