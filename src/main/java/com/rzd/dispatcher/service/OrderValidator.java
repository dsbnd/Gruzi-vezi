package com.rzd.dispatcher.service;

import com.rzd.dispatcher.model.dto.request.CreateOrderRequest;
import com.rzd.dispatcher.model.enums.CargoType;
import com.rzd.dispatcher.model.enums.PackagingType;
import com.rzd.dispatcher.model.enums.WagonType;
import org.springframework.stereotype.Service;

@Service
public class OrderValidator {

    public void validate(CreateOrderRequest request) {
        CargoType cargo = request.getCargo().getCargoType();
        PackagingType packaging = request.getCargo().getPackagingType();
        WagonType wagon = request.getRequestedWagonType();

        // Универсальный метод проверки на цистерну (учитываем оба регистра)
        boolean isTankWagon = wagon == WagonType.цистерна || wagon == WagonType.ЦИСТЕРНА;
        boolean isPlatform = wagon == WagonType.платформа || wagon == WagonType.ПЛАТФОРМА;
        boolean isOpenTop = wagon == WagonType.полувагон || wagon == WagonType.ПОЛУВАГОН;

        // ПРАВИЛО 1: Наливные грузы только в цистернах (или бочках в крытом, но для простоты берем строгий вариант)
        if ((cargo == CargoType.Нефть || cargo == CargoType.Наливной) && !isTankWagon) {
            throw new IllegalArgumentException("Наливные грузы (Нефть) можно перевозить только в цистернах!");
        }

        // ПРАВИЛО 2: В цистерну нельзя грузить твердую упаковку
        if (isTankWagon && (packaging == PackagingType.Паллеты || packaging == PackagingType.Ящики || packaging == PackagingType.Мешки || packaging == PackagingType.Контейнеры)) {
            throw new IllegalArgumentException("В цистерну нельзя погрузить груз в паллетах, ящиках или контейнерах!");
        }

        // ПРАВИЛО 3: Защита электроники
        if ((cargo == CargoType.Электроника || cargo == CargoType.Хрупкий) && packaging == PackagingType.Без_упаковки) {
            throw new IllegalArgumentException("Электронику и хрупкие грузы нельзя перевозить без упаковки!");
        }
        if ((cargo == CargoType.Электроника) && (isPlatform || isOpenTop) && packaging != PackagingType.Контейнеры) {
            throw new IllegalArgumentException("Электронику на платформе или в полувагоне можно перевозить только в контейнерах!");
        }

        // ПРАВИЛО 4: Уголь и сыпучие
        if (cargo == CargoType.Уголь && (packaging == PackagingType.Коробки || packaging == PackagingType.Паллеты)) {
            throw new IllegalArgumentException("Уголь перевозится навалом или без упаковки, использование коробок/паллет запрещено!");
        }

        // ПРАВИЛО 5: Контейнеры не влезут в крытый вагон
        if (packaging == PackagingType.Контейнеры && !isPlatform && !isOpenTop) {
            throw new IllegalArgumentException("Контейнеры устанавливаются только на платформы или в полувагоны!");
        }
    }
}