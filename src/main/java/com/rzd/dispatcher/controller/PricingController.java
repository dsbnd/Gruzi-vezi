package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatcher/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    /**
     * 1. Базовый расчет стоимости
     * POST /api/dispatcher/pricing/calculate
     * Body: {
     *   "cargoType": "Электроника",
     *   "wagonType": "крытый",
     *   "weightKg": 10000,
     *   "departureStation": "Москва-Товарная",
     *   "destinationStation": "Екатеринбург-Товарный",
     *   "selectedServices": ["INSURANCE", "TRACKING"]
     * }
     */
    @PostMapping("/calculate")
    public ResponseEntity<PriceResponse> calculatePrice(
            @Valid @RequestBody PriceCalculationRequest request) {
        PriceResponse response = pricingService.calculatePrice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Полный расчет с конкретным вагоном
     * POST /api/dispatcher/pricing/full?orderId=...&wagonId=...
     * Body: {"selectedServices": ["INSURANCE", "ESCORT", "TRACKING"]}
     */
    @PostMapping("/full")
    public ResponseEntity<PriceResponse> calculateFullPrice(
            @RequestParam UUID orderId,
            @RequestParam UUID wagonId,
            @RequestBody(required = false) SelectedServicesRequest selectedServices) {

        Set<String> services = selectedServices != null ?
                selectedServices.getSelectedServices() : null;

        PriceResponse response = pricingService.calculateFullPrice(
                orderId, wagonId, services);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. Расчет для заказа (без вагона, только оценка)
     * GET /api/dispatcher/pricing/estimate?orderId=...&wagonType=крытый
     */
    @GetMapping("/estimate")
    public ResponseEntity<PriceResponse> estimatePrice(
            @RequestParam UUID orderId,
            @RequestParam String wagonType) {
        PriceResponse response = pricingService.calculateEstimatedPrice(orderId, wagonType);
        return ResponseEntity.ok(response);
    }

    /**
     * Вспомогательный класс для тела запроса
     */
    public static class SelectedServicesRequest {
        private Set<String> selectedServices;

        public Set<String> getSelectedServices() {
            return selectedServices;
        }

        public void setSelectedServices(Set<String> selectedServices) {
            this.selectedServices = selectedServices;
        }
    }
}