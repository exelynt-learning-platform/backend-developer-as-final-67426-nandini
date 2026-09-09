package com.example.demo.repository;

import com.example.demo.entity.Reservation;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    List<Reservation> findByUser(User user);
    List<Reservation> findByUserId(Long userId);
    boolean existsByResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(Long resourceId, LocalDateTime endTime, LocalDateTime startTime);
    boolean existsByResourceIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(Long resourceId, LocalDateTime endTime, LocalDateTime startTime, ReservationStatus status);
    List<Reservation> findByResourceIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(Long resourceId, LocalDateTime endTime, LocalDateTime startTime, ReservationStatus status);
}