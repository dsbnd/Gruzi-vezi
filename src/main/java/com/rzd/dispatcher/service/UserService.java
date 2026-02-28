package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.dto.response.UserProfileResponse;
import com.rzd.dispatcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .companyName(user.getCompanyName())
                .inn(user.getInn())
                .role(user.getRole().name())
                .build();
    }
}