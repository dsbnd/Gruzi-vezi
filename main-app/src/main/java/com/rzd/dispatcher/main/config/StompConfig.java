package com.rzd.dispatcher.main.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@Slf4j
@Configuration
public class StompConfig {

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitmqPassword;

    @Bean
    public StompSession stompSession() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new org.springframework.messaging.converter.StringMessageConverter());

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{10000, 10000});

        String url = "ws://" + rabbitmqHost + ":15674/ws";

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.setLogin(rabbitmqUsername);
        connectHeaders.setPasscode(rabbitmqPassword);

        return stompClient.connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                log.info("STOMP успешно подключен к RabbitMQ!");
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                log.error("Ошибка STOMP: {}", exception.getMessage());
            }
        }).get();
    }
}