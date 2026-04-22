package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.response.OrderResponse;
import com.rzd.dispatcher.model.entity.Wagon;
import com.rzd.dispatcher.model.enums.WagonStatus;
import com.rzd.dispatcher.repository.WagonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WagonAdminService {
    private final WagonRepository wagonRepository;

    @Transactional
    public Wagon addWagon(Wagon wagon) {
        return wagonRepository.save(wagon);
    }

    @Transactional
    public void updateStatus(UUID id, WagonStatus status) {
        wagonRepository.updateStatus(id, status); 
    }

    @Transactional
    public List<Wagon> getAllWagons(){
        return wagonRepository.findAll();
    }

    @Transactional
    public void deleteWagon(UUID id) {
        wagonRepository.deleteById(id);
    }
}