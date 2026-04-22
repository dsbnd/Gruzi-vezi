package com.rzd.dispatcher.main.controller;

import com.rzd.dispatcher.main.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "http://localhost:5173")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/search-free")
    public List<Map<String, Object>> searchStationsWithFreeWagons(@RequestParam String query) {
        return stationService.searchStationsWithFreeWagons(query);
    }
}