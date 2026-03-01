package com.rzd.dispatcher.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class StationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findStationsByName(String query) {
        String sql = "SELECT DISTINCT name FROM (" +
                "SELECT current_station as name FROM wagons " +  // ← ДОБАВИЛИ ЭТУ СТРОКУ
                "UNION " +
                "SELECT departure_station as name FROM wagon_schedule " +
                "UNION " +
                "SELECT arrival_station FROM wagon_schedule " +
                "UNION " +
                "SELECT from_station FROM station_distances " +
                "UNION " +
                "SELECT to_station FROM station_distances" +
                ") all_stations " +
                "WHERE LOWER(name) LIKE LOWER(?) LIMIT 10";

        return jdbcTemplate.queryForList(sql, "%" + query + "%");
    }
}