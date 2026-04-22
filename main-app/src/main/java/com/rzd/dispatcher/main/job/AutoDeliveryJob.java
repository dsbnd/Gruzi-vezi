package com.rzd.dispatcher.main.job;

import com.rzd.dispatcher.common.model.entity.Order;
import com.rzd.dispatcher.common.model.entity.Wagon;
import com.rzd.dispatcher.common.model.enums.OrderStatus;
import com.rzd.dispatcher.common.model.enums.WagonStatus;
import com.rzd.dispatcher.common.repository.OrderRepository;
import com.rzd.dispatcher.common.repository.WagonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoDeliveryJob extends QuartzJobBean {

    private final OrderRepository orderRepository;
    private final WagonRepository wagonRepository;

    @Override
    @Transactional
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("⏳ Запуск планировщика Quartz: проверка прибывающих поездов...");

        // 1. Находим все заявки, которые сейчас в пути
        List<Order> ordersInTransit = orderRepository.findByStatus(OrderStatus.в_пути);

        if (ordersInTransit.isEmpty()) {
            log.info("Нет поездов в пути. Планировщик завершил работу.");
            return;
        }

        for (Order order : ordersInTransit) {
            // 2. Меняем статус заявки на "доставлен"
            order.setStatus(OrderStatus.доставлен);
            orderRepository.save(order);

            // 3. Освобождаем привязанный вагон и меняем его станцию
            Wagon wagon = order.getWagon(); // <-- Проверь, как у тебя называется связь заказа с вагоном
            if (wagon != null) {
                wagon.setStatus(WagonStatus.свободен);
                wagon.setCurrentStation(order.getDestinationStation()); // Вагон остается на станции назначения
                wagonRepository.save(wagon);
            }

            log.info("Рейс для заявки {} завершен! Вагон прибыл на станцию {}.",
                    order.getId(), order.getDestinationStation());
        }
    }
}