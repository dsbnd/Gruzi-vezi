package com.rzd.dispatcher.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private int status;         // HTTP статус (400, 401 и т.д.)
    private String message;     // Понятное сообщение для пользователя
    private LocalDateTime time; // Время ошибки
}