package vn.hoang.datn92demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hoang.datn92demo.model.AreaSubscription;

import java.util.List;

public interface AreaSubscriptionRepository extends JpaRepository<AreaSubscription, Long> {

    // Lấy subscription theo areaId (không fetch join)
    List<AreaSubscription> findByArea_Id(Long areaId);

    // Lấy subscription theo userId (không fetch join)
    List<AreaSubscription> findByUser_Id(Long userId);

    // -----------------------------
    // Fetch join để tránh lazy init
    // -----------------------------

    @Query("""
           SELECT s FROM AreaSubscription s
           LEFT JOIN FETCH s.area
           LEFT JOIN FETCH s.user
           """)
    List<AreaSubscription> findAllWithAreaAndUser();

    @Query("""
           SELECT s FROM AreaSubscription s 
           LEFT JOIN FETCH s.area 
           LEFT JOIN FETCH s.user
           WHERE s.area.id = :areaId
           """)
    List<AreaSubscription> findByAreaIdWithUserAndArea(@Param("areaId") Long areaId);

    @Query("""
           SELECT s FROM AreaSubscription s 
           LEFT JOIN FETCH s.area 
           LEFT JOIN FETCH s.user
           WHERE s.user.id = :userId
           """)
    List<AreaSubscription> findByUserIdWithArea(@Param("userId") Long userId);
}
