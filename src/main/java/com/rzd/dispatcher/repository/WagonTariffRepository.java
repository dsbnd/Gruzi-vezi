package com.rzd.dispatcher.repository;

import com.rzd.dispatcher.model.entity.WagonTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WagonTariffRepository extends JpaRepository<WagonTariff, UUID> {

    Optional<WagonTariff> findByWagonTypeAndCargoType(String wagonType, String cargoType);
}