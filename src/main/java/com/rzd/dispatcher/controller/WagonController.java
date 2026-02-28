package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.WagonSearchRequest;
import com.rzd.dispatcher.model.dto.response.WagonAvailabilityResponse;
import com.rzd.dispatcher.service.WagonSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    // ДОБАВЛЯЕМ ЭНДПОИНТ ДЛЯ РЕЗЕРВИРОВАНИЯ
    @PostMapping("/{wagonId}/reserve")
    public ResponseEntity<String> reserveWagon(
            @PathVariable UUID wagonId,
            @RequestParam UUID orderId,
            @RequestParam(defaultValue = "30") int minutes) {

        boolean reserved = wagonSearchService.reserveWagon(wagonId, orderId, minutes);
        if (reserved) {
            return ResponseEntity.ok("Вагон успешно зарезервирован на " + minutes + " минут");
        } else {
            return ResponseEntity.badRequest().body("Вагон уже зарезервирован");
        }
    }

    // ДОБАВЛЯЕМ ЭНДПОИНТ ДЛЯ ОСВОБОЖДЕНИЯ
    @PostMapping("/{wagonId}/release")
    public ResponseEntity<String> releaseWagon(@PathVariable UUID wagonId) {
        wagonSearchService.releaseWagon(wagonId);
        return ResponseEntity.ok("Вагон освобожден");
    }
}