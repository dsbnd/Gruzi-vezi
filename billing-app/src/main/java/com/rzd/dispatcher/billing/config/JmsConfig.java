package com.rzd.dispatcher.billing.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.BeanFactoryDestinationResolver;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableJms
public class JmsConfig {

    public static final String ORDER_COMPLETED_QUEUE = "order.completed.v6";

    @Bean
    public ConnectionFactory jmsConnectionFactory() {
        RMQConnectionFactory factory = new RMQConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }

    // AMQP-native destination: читаем сырой AMQP body без JMS-конверта.
    // Нужен, т.к. продюсер — STOMP (не-JMS), и RMQ-JMS иначе пытается ObjectInputStream-ить тело.
    @Bean(name = "orderCompletedQueue")
    public Destination orderCompletedQueue() {
        RMQDestination d = new RMQDestination();
        d.setDestinationName(ORDER_COMPLETED_QUEUE);
        d.setAmqp(true);
        d.setAmqpExchangeName("");
        d.setAmqpRoutingKey(ORDER_COMPLETED_QUEUE);
        d.setAmqpQueueName(ORDER_COMPLETED_QUEUE);
        return d;
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
            MessageConverter rawStringMessageConverter,
            BeanFactory beanFactory) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setMessageConverter(rawStringMessageConverter);
        // @JmsListener(destination="orderCompletedQueue") найдёт Destination-бин, а не создаст новый с amqp=false
        factory.setDestinationResolver(new BeanFactoryDestinationResolver(beanFactory));
        return factory;
    }
}