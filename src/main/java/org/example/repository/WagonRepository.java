package org.example.repository;

import org.example.model.entity.Wagon;
import org.example.model.enums.WagonStatus;
import org.example.model.enums.WagonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WagonRepository extends JpaRepository<Wagon, UUID> {

    // Базовые поиски
    List<Wagon> findByStatus(WagonStatus status);

    List<Wagon> findByWagonType(WagonType wagonType);

    List<Wagon> findByWagonTypeAndStatus(WagonType wagonType, WagonStatus status);

    Optional<Wagon> findByWagonNumber(String wagonNumber);

    // Сложный поиск доступных вагонов
    @Query("SELECT w FROM Wagon w WHERE w.status = 'свободен' " +
            "AND w.maxWeightKg >= :weight " +
            "AND w.maxVolumeM3 >= :volume " +
            "AND w.currentStation = :station")
    List<Wagon> findAvailableWagons(@Param("station") String station,
                                    @Param("weight") Integer weight,
                                    @Param("volume") Integer volume);

    // Поиск с пессимистичной блокировкой для резервирования
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wagon w WHERE w.id = :id")
    Optional<Wagon> findByIdForUpdate(@Param("id") UUID id);

    // Обновление статуса вагона
    @Modifying
    @Query("UPDATE Wagon w SET w.status = :status WHERE w.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") WagonStatus status);

    // Статистика по вагонам
    @Query("SELECT w.status, COUNT(w) FROM Wagon w GROUP BY w.status")
    List<Object[]> getWagonStatistics();
}