package com.rzd.dispatcher.billing.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzd.dispatcher.billing.jca.WmsConnectionFactory;
import com.rzd.dispatcher.billing.jca.WmsConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import jakarta.resource.ResourceException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedListener {

    private final RestTemplate restTemplate;
    private final WmsConnectionFactory wmsConnectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @JmsListener(destination = "orderCompletedQueue")
    public void receiveMessage(String payload) {
        log.info("[JMS Listener] STOMP‑сообщение декодировано: {}", payload);

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String orderId = rootNode.get("orderId").asText();

            String mainAppUrl = "http://RZD-DISPATCHER/api/orders/" + orderId;
            String orderDetailsJson = restTemplate.getForObject(mainAppUrl, String.class);
            log.info("Детали заказа: {}", orderDetailsJson);

            String xmlShippingNote = generateXmlShippingNote(orderId, orderDetailsJson);

            try (WmsConnection connection = wmsConnectionFactory.getConnection()) {
                connection.sendShippingNote(orderId, xmlShippingNote);
                log.info("Накладная успешно отправлена в WMS через JCA");
//                throw new RuntimeException("ТЕСТ XA: проверка отката транзакции");
            } catch (ResourceException e) {
                log.error("Ошибка JCA при отправке в WMS", e);
                throw new RuntimeException("JCA WMS error", e);
            }

        } catch (Exception e) {
            log.error("Критическая ошибка в Listener", e);
            throw new RuntimeException(e);
        }
    }

    private String generateXmlShippingNote(String orderId, String orderJson) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<shippingNote>\n" +
                        "  <orderId>%s</orderId>\n" +
                        "  <details>%s</details>\n" +
                        "  <timestamp>%s</timestamp>\n" +
                        "</shippingNote>",
                orderId, orderJson, java.time.Instant.now().toString()
        );
    }
}