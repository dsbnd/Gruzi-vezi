package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.StationDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationDistanceRepository extends JpaRepository<StationDistance, UUID> {

    // Поиск расстояния между двумя станциями
    Optional<StationDistance> findByFromStationAndToStation(String fromStation, String toStation);

    // Поиск всех расстояний от станции
    List<StationDistance> findByFromStation(String fromStation);

    // Поиск всех расстояний до станции
    List<StationDistance> findByToStation(String toStation);

    // Проверка существования
    boolean existsByFromStationAndToStation(String fromStation, String toStation);

    // Удаление записи
    void deleteByFromStationAndToStation(String fromStation, String toStation);

    // Поиск ближайших станций (с расстоянием меньше заданного)
    @Query("SELECT sd FROM StationDistance sd WHERE " +
            "sd.fromStation = :station AND sd.distanceKm <= :maxDistance")
    List<StationDistance> findNearbyStations(@Param("station") String station,
                                             @Param("maxDistance") Integer maxDistance);
}