-- =====================================================
-- 1. Удаляем старые ограничения
-- =====================================================

-- Удаляем уникальное ограничение с inn (теперь один ИНН может иметь много счетов)
ALTER TABLE company_accounts DROP CONSTRAINT IF EXISTS company_accounts_inn_key;

-- =====================================================
-- 2. Добавляем новые поля (если их нет)
-- =====================================================

-- Добавляем поле is_main (флаг основного счета)
ALTER TABLE company_accounts
ADD COLUMN IF NOT EXISTS is_main BOOLEAN DEFAULT false;

-- Добавляем поле is_rzd_account (флаг счета РЖД)
ALTER TABLE company_accounts
ADD COLUMN IF NOT EXISTS is_rzd_account BOOLEAN DEFAULT false;

-- =====================================================
-- 3. Создаем новые ограничения и индексы
-- =====================================================

-- Создаем составной уникальный ключ (inn + account_number)
-- Это гарантирует, что у одного ИНН не будет дублей номеров счетов
ALTER TABLE company_accounts
DROP CONSTRAINT IF EXISTS company_accounts_inn_account_unique;

ALTER TABLE company_accounts
ADD CONSTRAINT company_accounts_inn_account_unique
UNIQUE (inn, account_number);

-- Создаем индекс для быстрого поиска счетов РЖД
CREATE INDEX IF NOT EXISTS idx_company_accounts_rzd
ON company_accounts(is_rzd_account)
WHERE is_rzd_account = true;

-- Создаем индекс для поиска основного счета компании
CREATE INDEX IF NOT EXISTS idx_company_accounts_main
ON company_accounts(inn, is_main)
WHERE is_main = true;

-- Обновляем существующий индекс на inn (делаем его не уникальным)
DROP INDEX IF EXISTS idx_company_accounts_inn;
CREATE INDEX IF NOT EXISTS idx_company_accounts_inn ON company_accounts(inn);

-- =====================================================
-- 4. Создаем или обновляем счет РЖД
-- =====================================================

-- Вставляем или обновляем счет РЖД
INSERT INTO company_accounts (
    inn,
    company_name,
    account_number,
    balance,
    bik,
    bank_name,
    is_main,
    is_rzd_account,
    created_at,
    updated_at
) VALUES (
    '7708503727',
    'ОАО РЖД (Грузовые перевозки)',
    '40702810900000000001',
    10000000.00,
    '044525225',
    'ПАО СБЕРБАНК',
    true,   -- is_main
    true,   -- is_rzd_account
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (account_number) DO UPDATE SET
    is_rzd_account = true,
    is_main = true,
    company_name = EXCLUDED.company_name,
    updated_at = CURRENT_TIMESTAMP;

-- =====================================================
-- 5. Добавляем тестовые данные (несколько счетов для одной компании)
-- =====================================================

-- ООО Вектор (ИНН: 7701234567) - несколько счетов
INSERT INTO company_accounts (
    inn,
    company_name,
    account_number,
    balance,
    bik,
    bank_name,
    is_main,
    is_rzd_account,
    created_at,
    updated_at
) VALUES
-- Основной счет в Сбербанке
('7701234567', 'ООО Вектор', '40702810123450000001', 500000.00, '044525225', 'ПАО СБЕРБАНК', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Дополнительный счет в Альфа-банке
('7701234567', 'ООО Вектор', '40702810765430000002', 250000.00, '044525111', 'АО АЛЬФА-БАНК', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Дополнительный счет в ВТБ
('7701234567', 'ООО Вектор', '40702810987650000003', 100000.00, '044525593', 'ПАО ВТБ', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_number) DO NOTHING;

-- ЗАО Ромашка (ИНН: 7709876543) - несколько счетов
INSERT INTO company_accounts (
    inn,
    company_name,
    account_number,
    balance,
    bik,
    bank_name,
    is_main,
    is_rzd_account,
    created_at,
    updated_at
) VALUES
-- Основной счет в Сбербанке
('7709876543', 'ЗАО Ромашка', '40702810234560000004', 750000.00, '044525225', 'ПАО СБЕРБАНК', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Дополнительный счет в Альфа-банке
('7709876543', 'ЗАО Ромашка', '40702810765430000005', 120000.00, '044525111', 'АО АЛЬФА-БАНК', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_number) DO NOTHING;

-- =====================================================
-- 6. Обновляем триггер для создания счета при регистрации пользователя
-- =====================================================

-- Удаляем старый триггер и функцию
DROP TRIGGER IF EXISTS after_user_insert ON users;
DROP FUNCTION IF EXISTS create_account_for_new_user();

-- Создаем новую функцию
-- Обновляем функцию для создания счета при регистрации пользователя
CREATE OR REPLACE FUNCTION create_account_for_new_user()
RETURNS TRIGGER AS $$
DECLARE
    new_account_number VARCHAR(20);
    counter INTEGER := 0;
BEGIN
    -- Генерируем уникальный номер счета
    LOOP
        new_account_number := '40702810' ||
                              substring(NEW.inn from 1 for 8) ||
                              lpad(floor(random() * 10000)::text, 4, '0');

        EXIT WHEN NOT EXISTS (SELECT 1 FROM company_accounts WHERE account_number = new_account_number);

        counter := counter + 1;
        IF counter > 10 THEN
            new_account_number := '40702810' || lpad(floor(random() * 10000000000::bigint)::text, 12, '0');
            EXIT;
        END IF;
    END LOOP;

    -- ВСТАВЛЯЕМ НОВЫЙ СЧЕТ С БАЛАНСОМ 50 МЛН
    INSERT INTO company_accounts (
        inn,
        company_name,
        account_number,
        balance,
        bik,
        bank_name,
        is_main,
        is_rzd_account,
        created_at,
        updated_at
    ) VALUES (
        NEW.inn,
        NEW.company_name,
        new_account_number,
        50000000.00,  -- ← 50 МИЛЛИОНОВ РУБЛЕЙ
        '044525225',
        'ПАО СБЕРБАНК',
        true,
        false,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Создаем триггер на добавление пользователя
CREATE TRIGGER after_user_insert
    AFTER INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION create_account_for_new_user();

-- =====================================================
-- 7. Добавляем комментарии к таблице и полям
-- =====================================================

COMMENT ON TABLE company_accounts IS 'Банковские счета компаний (один ИНН может иметь несколько счетов)';
COMMENT ON COLUMN company_accounts.inn IS 'ИНН компании (10 или 12 цифр)';
COMMENT ON COLUMN company_accounts.account_number IS 'Номер счета (20 цифр)';
COMMENT ON COLUMN company_accounts.balance IS 'Текущий баланс счета';
COMMENT ON COLUMN company_accounts.is_main IS 'Флаг основного счета компании';
COMMENT ON COLUMN company_accounts.is_rzd_account IS 'Флаг счета РЖД (только один такой счет)';

ALTER TABLE payments
ADD COLUMN IF NOT EXISTS metadata TEXT;
COMMENT ON COLUMN payments.metadata IS 'Дополнительная информация о транзакции (JSON или текст)';