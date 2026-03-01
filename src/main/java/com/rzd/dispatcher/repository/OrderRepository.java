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

    // Поиск по пользователю
    List<Order> findByUserId(UUID userId);
    List<Order> findByUser_Email(String email);
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status);

    // Поиск по статусу
    List<Order> findByStatus(OrderStatus status);

    // Поиск по датам
    List<Order> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    // Поиск по станциям
    List<Order> findByDepartureStation(String station);

    List<Order> findByDestinationStation(String station);

    // Поиск по вагону
    List<Order> findByWagonId(UUID wagonId);

    // Обновление статуса
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    int updateStatus(@Param("orderId") UUID orderId, @Param("status") OrderStatus status);

    // Обновление цены
    @Modifying
    @Query("UPDATE Order o SET o.totalPrice = :price WHERE o.id = :orderId")
    int updatePrice(@Param("orderId") UUID orderId, @Param("price") BigDecimal price);

    // Статистика по заказам
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatistics();

    // Сумма всех заказов за период
    @Query("SELECT SUM(o.totalPrice) FROM Order o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "AND o.status = 'доставлен'")
    BigDecimal getTotalRevenue(@Param("start") OffsetDateTime start,
                               @Param("end") OffsetDateTime end);
}