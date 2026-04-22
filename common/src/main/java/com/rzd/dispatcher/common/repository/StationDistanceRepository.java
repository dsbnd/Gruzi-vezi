package com.rzd.dispatcher.common.repository;

import com.rzd.dispatcher.common.model.entity.StationDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationDistanceRepository extends JpaRepository<StationDistance, UUID> {

    
    Optional<StationDistance> findByFromStationAndToStation(String fromStation, String toStation);

    
    List<StationDistance> findByFromStation(String fromStation);

    
    List<StationDistance> findByToStation(String toStation);

    
    boolean existsByFromStationAndToStation(String fromStation, String toStation);

    
    void deleteByFromStationAndToStation(String fromStation, String toStation);

    
    @Query("SELECT sd FROM StationDistance sd WHERE " +
            "sd.fromStation = :station AND sd.distanceKm <= :maxDistance")
    List<StationDistance> findNearbyStations(@Param("station") String station,
                                             @Param("maxDistance") Integer maxDistance);
}