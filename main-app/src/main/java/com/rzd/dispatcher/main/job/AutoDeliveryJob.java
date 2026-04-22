package com.rzd.dispatcher.main.job;

import com.rzd.dispatcher.common.model.entity.Order;
import com.rzd.dispatcher.common.model.entity.Wagon;
import com.rzd.dispatcher.common.model.enums.OrderStatus;
import com.rzd.dispatcher.common.model.enums.WagonStatus;
import com.rzd.dispatcher.common.repository.OrderRepository;
import com.rzd.dispatcher.common.repository.WagonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.util.MimeTypeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoDeliveryJob extends QuartzJobBean {

    private final OrderRepository orderRepository;
    private final WagonRepository wagonRepository;
    private final StompSession stompSession;
    @Override
    @SchedulerLock(name = "AutoDeliveryJob", lockAtLeastFor = "PT10S")
    @Transactional
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("Запуск планировщика Quartz: проверка прибывающих поездов...");

        List<Order> ordersInTransit = orderRepository.findByStatus(OrderStatus.в_пути);

        if (ordersInTransit.isEmpty()) {
            log.info("Нет поездов в пути. Планировщик завершил работу.");
            return;
        }

        for (Order order : ordersInTransit) {
            // Меняем статус заявки на "доставлен"
            order.setStatus(OrderStatus.доставлен);
            orderRepository.save(order);

            // Освобождаем привязанный вагон и меняем его станцию
            Wagon wagon = order.getWagon();
            if (wagon != null) {
                wagon.setStatus(WagonStatus.свободен);
                wagon.setCurrentStation(order.getDestinationStation());
                wagonRepository.save(wagon);
            }

            log.info("Рейс для заявки {} завершен! Вагон прибыл на станцию {}.",
                    order.getId(), order.getDestinationStation());

            sendOrderCompletedEvent(order);
        }
    }

    /**
     * Отправка события о завершении заказа через STOMP в RabbitMQ
     */
    private void sendOrderCompletedEvent(Order order) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getId().toString());

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(payload);

            // МАГИЯ ЗДЕСЬ: Явно указываем брокеру, что это ПРОСТОЙ ТЕКСТ
            StompHeaders headers = new StompHeaders();
            headers.setDestination("/queue/order.completed.v6"); // Новая чистая очередь
            headers.setContentType(MimeTypeUtils.TEXT_PLAIN); // Указываем тип контента!

            // Отправляем сообщение вместе с заголовками
            stompSession.send(headers, jsonString);

            log.info("✉️ STOMP сообщение отправлено в очередь v6: {}", jsonString);
        } catch (Exception e) {
            log.error("❌ Ошибка отправки STOMP сообщения", e);
        }
    }
}