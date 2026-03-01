package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.service.StationService;
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

    @GetMapping("/search")
    public List<Map<String, Object>> searchStations(@RequestParam String query) {
        return stationService.searchStations(query);
    }
}