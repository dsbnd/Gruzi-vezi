package com.rzd.dispatcher.billing.jca;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EisConnectorMock {

    // Аннотация @Transactional заставит Narayana (JTA) управлять этой транзакцией
    // вместе с транзакцией базы данных и JMS (Two-Phase Commit / XA)
    @Transactional
    public void sendActToAccounting(String orderId) {
        log.info("🌐 [JCA/JTA] Инициация XA-транзакции...");
        log.info("💼 [JCA/EIS] Отправка акта выполненных работ для заказа {} в систему ЕИС Бухгалтерии...", orderId);
        
        // Симулируем сетевую задержку внешнего API
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Симуляция успешного ответа
        log.info("✅ [JCA/EIS] Акт для заказа {} успешно зарегистрирован в ЕИС!", orderId);
    }
}