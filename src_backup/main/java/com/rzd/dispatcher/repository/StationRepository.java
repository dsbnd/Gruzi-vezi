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

    public List<Map<String, Object>> findStationsWithFreeWagons(String query) {
        
        String sql = "SELECT DISTINCT current_station as name " +
                "FROM wagons " +
                "WHERE current_station ILIKE ? " +
                "AND status = 'свободен' " +
                "ORDER BY current_station " +
                "LIMIT 10";

        String searchPattern = "%" + query + "%";

        System.out.println("Searching with pattern: " + searchPattern);

        return jdbcTemplate.queryForList(sql, searchPattern);
    }
}