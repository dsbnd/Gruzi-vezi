package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.entity.User;
import com.rzd.dispatcher.model.dto.response.UserProfileResponse;
import com.rzd.dispatcher.model.dto.response.UserResponse;
import com.rzd.dispatcher.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        log.info("Загрузка профиля пользователя: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userType(user.getUserType())
                .role(user.getRole().name())
                .displayName(user.getDisplayName());

        if ("LEGAL_ENTITY".equals(user.getUserType())) {
            builder.companyName(user.getCompanyName())
                    .inn(user.getInn());
            log.info("Загружен профиль юридического лица: {}", user.getCompanyName());
        } else {
            builder.lastName(user.getLastName())
                    .firstName(user.getFirstName())
                    .patronymic(user.getPatronymic())
                    .phone(user.getPhone())
                    .passportSeries(user.getPassportSeries())
                    .passportNumber(user.getPassportNumber())
                    .passportIssuedBy(user.getPassportIssuedBy())
                    .passportIssuedDate(user.getPassportIssuedDate())
                    .registrationAddress(user.getRegistrationAddress())
                    .snils(user.getSnils())
                    .inn(user.getInn());
            log.info("Загружен профиль физического лица: {} {}",
                    user.getLastName(), user.getFirstName());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        log.info("Получение данных текущего пользователя: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .email(user.getEmail())
                .userType(user.getUserType())
                .displayName(user.getDisplayName());

        if ("LEGAL_ENTITY".equals(user.getUserType())) {
            builder.companyName(user.getCompanyName())
                    .inn(user.getInn());
            log.debug("Данные юрлица: {}", user.getCompanyName());
        } else {
            builder.firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .patronymic(user.getPatronymic())
                    .phone(user.getPhone())
                    .inn(user.getInn());
            log.debug("Данные физлица: {} {}", user.getLastName(), user.getFirstName());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.info("Получение списка всех пользователей");
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(UUID id) {
        log.info("Удаление пользователя с ID: {}", id);
        userRepository.deleteById(id);
        log.info("Пользователь {} успешно удален", id);
    }
}