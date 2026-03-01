package com.rzd.dispatcher.service;

import com.rzd.dispatcher.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public List<Map<String, Object>> searchStations(String query) {
        // Валидация входных данных
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        // Поиск станций
        return stationRepository.findStationsByName(query.trim());
    }
}