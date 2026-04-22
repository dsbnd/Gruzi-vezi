package com.rzd.dispatcher.billing.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableJms
public class JmsConfig {

    @Bean
    public ConnectionFactory jmsConnectionFactory() {
        RMQConnectionFactory factory = new RMQConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }

    // Абсолютно сырой конвертер, который не доверяет заголовкам
    @Bean
    public MessageConverter rawStringMessageConverter() {
        return new MessageConverter() {
            @Override
            public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
                return session.createTextMessage(object.toString());
            }

            @Override
            public Object fromMessage(Message message) throws JMSException, MessageConversionException {
                try {
                    // Если это уже текст - отлично
                    if (message instanceof TextMessage textMessage) {
                        return textMessage.getText();
                    }
                    // Если это байты (как от STOMP) - читаем сырые байты в UTF-8
                    if (message instanceof BytesMessage bytesMessage) {
                        byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                        bytesMessage.readBytes(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    }

                    // Если это ObjectMessage (именно он кидает 7B226F72), мы извлекаем байты ДО десериализации!
                    // RabbitMQ JMS Client прячет сырые байты внутри свойства "JMSBytes" (костыль для кросс-протоколов)
                    byte[] rawData = message.getBody(byte[].class);
                    if (rawData != null) {
                        return new String(rawData, StandardCharsets.UTF_8);
                    }

                } catch (Exception e) {
                    // Если не получилось прочитать тело, вернем хотя бы ID сообщения, чтобы не падать
                    return "Unparseable Message: " + message.getJMSMessageID();
                }
                return message.toString();
            }
        };
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rawStringMessageConverter) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);

        // ВНИМАНИЕ: Регистрируем наш конвертер
        factory.setMessageConverter(rawStringMessageConverter);
        return factory;
    }
}