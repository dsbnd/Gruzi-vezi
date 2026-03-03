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

    @PostMapping("/calculate")
    public ResponseEntity<PriceResponse> calculatePrice(
            @Valid @RequestBody PriceCalculationRequest request) {
        PriceResponse response = pricingService.calculatePrice(request);
        return ResponseEntity.ok(response);
    }

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


    @GetMapping("/estimate")
    public ResponseEntity<PriceResponse> estimatePrice(
            @RequestParam UUID orderId,
            @RequestParam String wagonType) {
        PriceResponse response = pricingService.calculateEstimatedPrice(orderId, wagonType);
        return ResponseEntity.ok(response);
    }

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