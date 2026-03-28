
CREATE TYPE public.user_type_enum AS ENUM (
    'INDIVIDUAL',  -- физическое лицо
    'LEGAL_ENTITY' -- юридическое лицо
);


ALTER TABLE public.users
    ADD COLUMN user_type user_type_enum NOT NULL DEFAULT 'LEGAL_ENTITY';


ALTER TABLE public.users
    ALTER COLUMN company_name DROP NOT NULL,
ALTER COLUMN inn DROP NOT NULL;


ALTER TABLE public.users
    ADD COLUMN first_name VARCHAR(255),
ADD COLUMN last_name VARCHAR(255),
ADD COLUMN patronymic VARCHAR(255),      -- отчество
ADD COLUMN phone VARCHAR(20),
ADD COLUMN passport_series VARCHAR(4),
ADD COLUMN passport_number VARCHAR(6),
ADD COLUMN passport_issued_by VARCHAR(255),
ADD COLUMN passport_issued_date DATE,
ADD COLUMN snils VARCHAR(14);             -- СНИЛС для физлиц


CREATE TABLE public.individual_documents (
                                             id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                                             user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
                                             document_type VARCHAR(50) NOT NULL,   -- 'PASSPORT', 'SNILS', 'INN_INDIVIDUAL'
                                             document_number VARCHAR(100) NOT NULL,
                                             file_url TEXT,
                                             verified BOOLEAN DEFAULT FALSE,
                                             verified_at TIMESTAMP WITH TIME ZONE,
                                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_individual_documents_updated_at
    BEFORE UPDATE ON public.individual_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

UPDATE public.users SET user_type = 'LEGAL_ENTITY' WHERE user_type IS NULL;


UPDATE public.users
SET user_type = 'INDIVIDUAL'
WHERE company_name IS NULL OR company_name = '';


CREATE OR REPLACE FUNCTION public.create_account_for_new_user()
RETURNS TRIGGER AS $$
DECLARE
new_account_number VARCHAR(20);
    counter INTEGER := 0;
    account_balance NUMERIC(15,2) := 50000000.00;
BEGIN
    IF NEW.user_type = 'INDIVIDUAL' THEN
        account_balance := 500000.00; -- 500 тыс. руб. для физлиц
END IF;

    LOOP
new_account_number := '40817810' ||
                              substring(NEW.inn from 1 for 8) ||
                              lpad(floor(random() * 10000)::text, 4, '0');

        EXIT WHEN NOT EXISTS (SELECT 1 FROM company_accounts WHERE account_number = new_account_number);

        counter := counter + 1;
        IF counter > 10 THEN
            new_account_number := '40817810' || lpad(floor(random() * 10000000000::bigint)::text, 12, '0');
            EXIT;
END IF;
END LOOP;

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
             COALESCE(NEW.company_name, NEW.last_name || ' ' || NEW.first_name),
             new_account_number,
             account_balance,
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
ALTER TABLE users
ALTER COLUMN user_type TYPE VARCHAR(50) USING user_type::text;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS registration_address TEXT;