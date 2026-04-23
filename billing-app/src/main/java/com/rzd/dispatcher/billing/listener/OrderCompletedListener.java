package com.rzd.dispatcher.billing.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzd.dispatcher.billing.jca.EisConnectorMock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedListener {

    private final EisConnectorMock eisConnector;
    private final RestTemplate restTemplate; // Наш балансировщик
    private final ObjectMapper objectMapper = new ObjectMapper();

    @JmsListener(destination = "orderCompletedQueue")
    public void receiveMessage(String payload) {
        log.info("🎉 📥 [JMS Listener] STOMP-сообщение успешно декодировано!");
        log.info("Содержимое: {}", payload);

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String orderId = rootNode.get("orderId").asText();

            // МАГИЯ БАЛАНСИРОВЩИКА: Мы пишем не "http://localhost:8080",
            // а имя сервиса из Eureka: "http://MAIN-APP"
            String mainAppUrl = "http://RZD-DISPATCHER/api/orders/" + orderId;

            log.info("🔍 Запрашиваем детали заказа у MAIN-APP...");
            // Делаем GET запрос
            String orderDetails = restTemplate.getForObject(mainAppUrl, String.class);
            log.info("📄 Детали заказа получены: {}", orderDetails);

            // Отправляем акт
            eisConnector.sendActToAccounting(orderId);

        } catch (Exception e) {
            log.error("❌ Ошибка при обработке или отправке акта", e);
            throw new RuntimeException(e);
        }
    }
}