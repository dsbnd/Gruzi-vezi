package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.request.PriceCalculationRequest;
import com.rzd.dispatcher.model.dto.response.PriceResponse;
import com.rzd.dispatcher.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}