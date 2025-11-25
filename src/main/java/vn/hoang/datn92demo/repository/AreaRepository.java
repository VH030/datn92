package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.hoang.datn92demo.model.Area;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {

    // Tìm khu vực theo mã code (để kiểm tra trùng)
    Optional<Area> findByCode(String code);

    // Nếu cần tìm theo tên, có thể thêm:
    Optional<Area> findByName(String name);
}
