package com.rzd.dispatcher.billing.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCompletedListener {

    // Чистая очередь v6
    @JmsListener(destination = "order.completed.v6")
    public void receiveMessage(String payload) {
        log.info("🎉 📥 [JMS Listener] STOMP-сообщение успешно декодировано!");
        log.info("Содержимое: {}", payload);
    }
}