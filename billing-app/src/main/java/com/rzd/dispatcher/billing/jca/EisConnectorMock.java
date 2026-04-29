package com.rzd.dispatcher.billing.jca;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EisConnectorMock {

    @Transactional
    public void sendActToAccounting(String orderId) {
        log.info("[JCA/JTA] Инициация XA-транзакции...");
        log.info("[JCA/EIS] Отправка акта выполненных работ для заказа {} в систему ЕИС Бухгалтерии...", orderId);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[JCA/EIS] Акт для заказа {} успешно зарегистрирован в ЕИС!", orderId);
    }
}