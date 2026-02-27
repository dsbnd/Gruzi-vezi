package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.WagonSearchRequest;
import com.rzd.dispatcher.model.dto.response.WagonAvailabilityResponse;
import com.rzd.dispatcher.service.WagonSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher/wagons")
@RequiredArgsConstructor
public class WagonController {

    private final WagonSearchService wagonSearchService;

    @PostMapping("/search")
    public ResponseEntity<List<WagonAvailabilityResponse>> searchWagons(
            @Valid @RequestBody WagonSearchRequest request) {

        List<WagonAvailabilityResponse> wagons = wagonSearchService.findAvailableWagons(request);
        return ResponseEntity.ok(wagons);
    }
}