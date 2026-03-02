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
                "WHERE LOWER(current_station) LIKE LOWER(?) " +
                "AND status = 'свободен' " +
                "LIMIT 10";

        return jdbcTemplate.queryForList(sql, "%" + query + "%");
    }
}