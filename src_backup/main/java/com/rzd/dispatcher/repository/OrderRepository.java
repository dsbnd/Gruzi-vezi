package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.Order;
import com.rzd.dispatcher.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    
    List<Order> findByUserId(UUID userId);
    List<Order> findByUser_Email(String email);
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    
    List<Order> findByStatus(OrderStatus status);

    
    List<Order> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    
    List<Order> findByDepartureStation(String station);

    List<Order> findByDestinationStation(String station);

    
    List<Order> findByWagonId(UUID wagonId);

    
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    int updateStatus(@Param("orderId") UUID orderId, @Param("status") OrderStatus status);

    
    @Modifying
    @Query("UPDATE Order o SET o.totalPrice = :price WHERE o.id = :orderId")
    int updatePrice(@Param("orderId") UUID orderId, @Param("price") BigDecimal price);

    
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatistics();

    
    @Query("SELECT SUM(o.totalPrice) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "AND o.status = 'доставлен'")
    BigDecimal getTotalRevenue(@Param("start") OffsetDateTime start,
                               @Param("end") OffsetDateTime end);
}