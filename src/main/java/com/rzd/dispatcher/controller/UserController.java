package com.rzd.dispatcher.controller;

import com.rzd.dispatcher.model.dto.response.UserProfileResponse;
import com.rzd.dispatcher.model.dto.response.UserResponse;
import com.rzd.dispatcher.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rzd.dispatcher.repository.UserRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        // Достаем email из токена
        String email = authentication.getName();

        // Ищем пользователя в БД
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Собираем красивый ответ для фронтенда
        UserResponse response = UserResponse.builder()
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .inn(String.valueOf(user.getInn()))
                .build();

        return ResponseEntity.ok(response);
    }
}