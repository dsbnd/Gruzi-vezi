CREATE TABLE wagon_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wagon_id UUID NOT NULL REFERENCES wagons(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    departure_station VARCHAR(255) NOT NULL,
    arrival_station VARCHAR(255) NOT NULL,
    departure_date TIMESTAMP WITH TIME ZONE,
    arrival_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'запланирован',
    cargo_type VARCHAR(255),
    cargo_weight_kg INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_schedule_wagon ON wagon_schedule(wagon_id);
CREATE INDEX idx_schedule_dates ON wagon_schedule(departure_date, arrival_date);
CREATE INDEX idx_schedule_status ON wagon_schedule(status);
CREATE TABLE wagon_tariffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wagon_type VARCHAR(50) NOT NULL,
    cargo_type VARCHAR(255) NOT NULL,
    base_rate_per_km DECIMAL(10,2) NOT NULL,
    coefficient DECIMAL(5,2) DEFAULT 1.0,
    min_price DECIMAL(10,2),
    description TEXT,
    UNIQUE(wagon_type, cargo_type)
);
CREATE TABLE station_distances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_station VARCHAR(255) NOT NULL,
    to_station VARCHAR(255) NOT NULL,
    distance_km INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(from_station, to_station)
);
CREATE INDEX idx_station_distances_from_to ON station_distances(from_station, to_station);
INSERT INTO wagon_tariffs (wagon_type, cargo_type, base_rate_per_km, coefficient, min_price) VALUES
('крытый', 'Электроника', 15.50, 1.2, 5000.00),
('крытый', 'общий', 12.00, 1.0, 4000.00),
('полувагон', 'Уголь', 8.50, 1.0, 3500.00),
('полувагон', 'Руда', 9.20, 1.1, 4000.00),
('платформа', 'Контейнеры', 11.00, 1.0, 4500.00),
('цистерна', 'Нефть', 14.00, 1.3, 6000.00);
INSERT INTO station_distances (from_station, to_station, distance_km) VALUES
('Москва-Товарная', 'Санкт-Петербург-Товарный', 650),
('Москва-Товарная', 'Екатеринбург-Товарный', 1667),
('Москва-Товарная', 'Новосибирск-Товарный', 2811),
('Москва-Товарная', 'Казань-Товарная', 797),
('Санкт-Петербург-Товарный', 'Москва-Товарная', 650),
('Санкт-Петербург-Товарный', 'Мурманск-Товарный', 1240),
('Екатеринбург-Товарный', 'Москва-Товарная', 1667),
('Екатеринбург-Товарный', 'Новосибирск-Товарный', 1520);
INSERT INTO wagon_tariffs (wagon_type, cargo_type, base_rate_per_km, coefficient, min_price) VALUES
('крытый', 'Электроника', 15.50, 1.2, 5000.00),
('крытый', 'Металл', 12.00, 1.1, 4500.00),
('крытый', 'Оборудование', 18.00, 1.4, 6000.00),
('крытый', 'общий', 14.00, 1.0, 4000.00),
('полувагон', 'Уголь', 8.50, 1.0, 3500.00),
('полувагон', 'Металл', 9.50, 1.1, 4000.00),
('полувагон', 'Лес', 9.00, 1.0, 3800.00),
('платформа', 'Контейнеры', 11.00, 1.0, 4500.00),
('платформа', 'Металл', 11.50, 1.1, 5000.00),
('цистерна', 'Нефть', 14.00, 1.3, 6000.00),
('цистерна', 'Химия', 13.50, 1.5, 6500.00)
ON CONFLICT (wagon_type, cargo_type)
DO UPDATE SET
    base_rate_per_km = EXCLUDED.base_rate_per_km,
    coefficient = EXCLUDED.coefficient,
    min_price = EXCLUDED.min_price;



