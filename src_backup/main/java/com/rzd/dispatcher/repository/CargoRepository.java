package com.rzd.dispatcher.repository;



import com.rzd.dispatcher.model.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, UUID> {

    Optional<Cargo> findByOrderId(UUID orderId);

    List<Cargo> findByCargoType(String cargoType);

    @Query("SELECT c FROM Cargo c WHERE c.weightKg > :minWeight")
    List<Cargo> findHeavyCargo(@Param("minWeight") Integer minWeight);

    @Query("SELECT AVG(c.weightKg) FROM Cargo c WHERE c.cargoType = :type")
    Double getAverageWeightByType(@Param("type") String type);
}