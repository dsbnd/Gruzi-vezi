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
import org.springframework.transaction.PlatformTransactionManager;

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
                    if (message instanceof TextMessage textMessage) {
                        return textMessage.getText();
                    }
                    if (message instanceof BytesMessage bytesMessage) {
                        byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                        bytesMessage.readBytes(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    }

                    byte[] rawData = message.getBody(byte[].class);
                    if (rawData != null) {
                        return new String(rawData, StandardCharsets.UTF_8);
                    }

                } catch (Exception e) {
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
            BeanFactory beanFactory,
            PlatformTransactionManager transactionManager) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        factory.setTransactionManager(transactionManager);
        factory.setMessageConverter(rawStringMessageConverter);
        factory.setDestinationResolver(new BeanFactoryDestinationResolver(beanFactory));
        return factory;
    }
}