package com.rzd.dispatcher.main.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Slf4j
@Configuration
public class StompConfig {

    @Bean
    public StompSession stompSession() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:15674/ws";  // порт STOMP WebSocket

        return stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, org.springframework.messaging.simp.stomp.StompHeaders headers) {
                log.info("STOMP подключен к RabbitMQ");
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("Ошибка STOMP", exception);
            }
        }).get();
    }
}