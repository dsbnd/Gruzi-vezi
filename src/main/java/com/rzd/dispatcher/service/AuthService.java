package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.LoginRequest;
import com.rzd.dispatcher.model.dto.request.RefreshTokenRequest;
import com.rzd.dispatcher.model.dto.request.RegisterRequest;
import com.rzd.dispatcher.model.dto.response.AuthResponse;
import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.enums.Role;
import com.rzd.dispatcher.repository.UserRepository;
import com.rzd.dispatcher.security.JwtService;
import com.rzd.dispatcher.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // Проверяем, нет ли уже такого пользователя
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        // Создаем нового пользователя
        var user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCompanyName(request.getCompanyName());
        user.setInn(request.getInn());
        user.setRole(Role.USER); // По умолчанию даем роль USER

        userRepository.save(user);

        // Генерируем токены
        var jwtToken = jwtService.generateAccessToken(user.getEmail());
        var refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Пытаемся аутентифицировать пользователя
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            // ПЕЧАТАЕМ РЕАЛЬНУЮ ОШИБКУ В КОНСОЛЬ!
            System.err.println("=== ОШИБКА АВТОРИЗАЦИИ ===");
            System.err.println("Тип ошибки: " + e.getClass().getName());
            System.err.println("Сообщение: " + e.getMessage());
            e.printStackTrace();

            throw new RuntimeException("Секретная ошибка: " + e.getMessage());
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден в БД"));

        var jwtToken = jwtService.generateAccessToken(user.getEmail());
        var refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        
        // Достаем email из Redis по рефреш токену
        String userEmail = refreshTokenService.getEmailByRefreshToken(requestRefreshToken);

        if (userEmail == null) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        // Если токен валидный, генерируем новый access токен
        var accessToken = jwtService.generateAccessToken(userEmail);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(requestRefreshToken) // Возвращаем старый рефреш, пока он не протух
                .build();
    }
}