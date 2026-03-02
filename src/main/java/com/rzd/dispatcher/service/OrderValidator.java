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

        int weightKg = request.getCargo().getWeightKg();
        int volumeM3 = request.getCargo().getVolumeM3();

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

        double density = (double) weightKg / volumeM3;

        switch (cargo) {
            case Металл:
            case Трубы_стальные:
                // Металл очень тяжелый. Даже трубы с пустотой внутри весят прилично.
                if (density < 800) {
                    throw new IllegalArgumentException(String.format("Ошибка: для металла указан слишком большой объем (%d м³) при таком весе (%d кг). Плотность слишком низкая.", volumeM3, weightKg));
                }
                break;

            case Лес:
                // Дерево обычно легче воды (1000 кг/м³), но 1 куб не может весить 10 кг.
                // Норма для леса: от 400 до 900 кг/м³
                if (density < 300 || density > 1000) {
                    throw new IllegalArgumentException("Ошибка в габаритах: Плотность леса обычно составляет от 300 до 1000 кг/м³. Проверьте введенные вес и объем.");
                }
                break;

            case Уголь:
            case Зерно:
                // Сыпучие грузы имеют предсказуемую насыпную плотность (примерно 600 - 900 кг/м³)
                if (density < 400 || density > 1200) {
                    throw new IllegalArgumentException(String.format("Ошибка: для груза '%s' указаны нереалистичные пропорции веса и объема.", cargo.name()));
                }
                break;

            case Нефть:
            case Химия:
            case Наливной:
                // Жидкости обычно весят около 700-1200 кг в одном кубе
                if (density < 600 || density > 1500) {
                    throw new IllegalArgumentException("Ошибка: плотность наливных грузов должна быть в районе 600-1500 кг/м³. Проверьте данные.");
                }
                break;

            default:
                // Для остальных (Электроника, Оборудование, Контейнеры) разброс может быть огромным из-за упаковки.
                // Но ставим жесткую защиту от "легче воздуха" и "тяжелее золота"
                if (density < 20) {
                    throw new IllegalArgumentException("Ошибка в габаритах: Груз получается почти невесомым (менее 20 кг в 1 м³)! Проверьте правильность ввода.");
                }
                if (density > 20000) {
                    throw new IllegalArgumentException("Ошибка в габаритах: Ваш груз тяжелее свинца! Проверьте вес и объем.");
                }
                break;
        }
    }
}