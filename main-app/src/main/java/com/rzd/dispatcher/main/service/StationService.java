package com.rzd.dispatcher.main.service;

import com.rzd.dispatcher.main.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public List<Map<String, Object>> searchStationsWithFreeWagons(String query) {
        
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        
        return stationRepository.findStationsWithFreeWagons(query.trim());
    }
}