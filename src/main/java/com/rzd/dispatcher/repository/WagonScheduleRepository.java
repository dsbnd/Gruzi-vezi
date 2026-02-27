package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.WagonSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WagonScheduleRepository extends JpaRepository<WagonSchedule, UUID> {

    List<WagonSchedule> findByWagonId(UUID wagonId);

    List<WagonSchedule> findByOrderId(UUID orderId);

    List<WagonSchedule> findByStatus(String status);

    @Query("SELECT ws FROM WagonSchedule ws WHERE " +
            "ws.wagon.id = :wagonId AND " +
            "ws.status IN ('запланирован', 'в_пути') AND " +
            "((ws.departureDate BETWEEN :start AND :end) OR " +
            "(ws.arrivalDate BETWEEN :start AND :end))")
    List<WagonSchedule> findConflictingSchedules(@Param("wagonId") UUID wagonId,
                                                 @Param("start") OffsetDateTime start,
                                                 @Param("end") OffsetDateTime end);
}