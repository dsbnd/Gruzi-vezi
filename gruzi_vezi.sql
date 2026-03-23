--
-- PostgreSQL database dump
--

-- Dumped from database version 17.4
-- Dumped by pg_dump version 17.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: order_status_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.order_status_enum AS ENUM (
    'черновик',
    'поиск_вагона',
    'ожидает_оплаты',
    'в_пути',
    'доставлен'
);


ALTER TYPE public.order_status_enum OWNER TO postgres;

--
-- Name: service_name_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.service_name_enum AS ENUM (
    'страхование',
    'сопровождение',
    'терминальная_обработка'
);


ALTER TYPE public.service_name_enum OWNER TO postgres;

--
-- Name: wagon_status_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.wagon_status_enum AS ENUM (
    'свободен',
    'забронирован',
    'в_пути',
    'на_ремонте'
);


ALTER TYPE public.wagon_status_enum OWNER TO postgres;

--
-- Name: wagon_type_enum; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.wagon_type_enum AS ENUM (
    'крытый',
    'полувагон',
    'платформа',
    'цистерна',
    'рефрижератор'
);


ALTER TYPE public.wagon_type_enum OWNER TO postgres;

--
-- Name: create_account_for_new_user(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_account_for_new_user() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.create_account_for_new_user() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: cargo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cargo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    cargo_type character varying(255) NOT NULL,
    weight_kg integer NOT NULL,
    volume_m3 integer NOT NULL,
    packaging_type character varying(100) NOT NULL
);


ALTER TABLE public.cargo OWNER TO postgres;

--
-- Name: company_accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company_accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    inn character varying(12) NOT NULL,
    company_name character varying(255) NOT NULL,
    account_number character varying(20) NOT NULL,
    balance numeric(15,2) DEFAULT 0.00,
    bik character varying(9) NOT NULL,
    bank_name character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    is_main boolean DEFAULT false,
    is_rzd_account boolean DEFAULT false
);


ALTER TABLE public.company_accounts OWNER TO postgres;

--
-- Name: TABLE company_accounts; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.company_accounts IS 'Банковские счета компаний (один ИНН может иметь несколько счетов)';


--
-- Name: COLUMN company_accounts.inn; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.company_accounts.inn IS 'ИНН компании (10 или 12 цифр)';


--
-- Name: COLUMN company_accounts.account_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.company_accounts.account_number IS 'Номер счета (20 цифр)';


--
-- Name: COLUMN company_accounts.balance; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.company_accounts.balance IS 'Текущий баланс счета';


--
-- Name: COLUMN company_accounts.is_main; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.company_accounts.is_main IS 'Флаг основного счета компании';


--
-- Name: COLUMN company_accounts.is_rzd_account; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.company_accounts.is_rzd_account IS 'Флаг счета РЖД (только один такой счет)';


--
-- Name: order_services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_services (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    service_name public.service_name_enum NOT NULL,
    price numeric(10,2) NOT NULL
);


ALTER TABLE public.order_services OWNER TO postgres;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    departure_station character varying(255) NOT NULL,
    destination_station character varying(255) NOT NULL,
    wagon_id uuid,
    status character varying(255) DEFAULT 'черновик'::public.order_status_enum,
    total_price numeric(10,2),
    carbon_footprint_kg numeric(10,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    requested_wagon_type character varying(255) NOT NULL
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    payment_id character varying(255),
    amount numeric(10,2) NOT NULL,
    status character varying(50) NOT NULL,
    payment_method character varying(100),
    company_name character varying(255),
    inn character varying(12),
    kpp character varying(9),
    bik character varying(9),
    account_number character varying(20),
    correspondent_account character varying(20),
    bank_name character varying(255),
    payment_purpose text,
    payment_document character varying(50),
    payment_date timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    paid_at timestamp with time zone,
    error_message text,
    metadata text
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- Name: COLUMN payments.inn; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.inn IS 'ИНН плательщика (10 или 12 цифр)';


--
-- Name: COLUMN payments.kpp; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.kpp IS 'КПП плательщика (9 цифр для юрлиц)';


--
-- Name: COLUMN payments.bik; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.bik IS 'БИК банка плательщика';


--
-- Name: COLUMN payments.account_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.account_number IS 'Расчетный счет плательщика';


--
-- Name: COLUMN payments.payment_document; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.payment_document IS 'Номер платежного документа (платежное поручение)';


--
-- Name: COLUMN payments.metadata; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.payments.metadata IS 'Дополнительная информация о транзакции (JSON или текст)';


--
-- Name: station_distances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.station_distances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    from_station character varying(255) NOT NULL,
    to_station character varying(255) NOT NULL,
    distance_km integer NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.station_distances OWNER TO postgres;

--
-- Name: tariffs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariffs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cargo_type character varying(255) NOT NULL,
    wagon_type character varying(255) NOT NULL,
    base_rate numeric(10,2) NOT NULL,
    coefficient numeric(5,2) DEFAULT 1.0,
    description text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tariffs OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    company_name character varying(255) NOT NULL,
    inn character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    role character varying(255) DEFAULT 'USER'::character varying NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: wagon_schedule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wagon_schedule (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    wagon_id uuid NOT NULL,
    order_id uuid,
    departure_station character varying(255) NOT NULL,
    arrival_station character varying(255) NOT NULL,
    departure_date timestamp with time zone,
    arrival_date timestamp with time zone,
    status character varying(50) DEFAULT 'запланирован'::character varying,
    cargo_type character varying(255),
    cargo_weight_kg integer,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.wagon_schedule OWNER TO postgres;

--
-- Name: wagon_tariffs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wagon_tariffs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    wagon_type character varying(50) NOT NULL,
    cargo_type character varying(255) NOT NULL,
    base_rate_per_km numeric(10,2) NOT NULL,
    coefficient numeric(5,2) DEFAULT 1.0,
    min_price numeric(10,2),
    description text
);


ALTER TABLE public.wagon_tariffs OWNER TO postgres;

--
-- Name: wagons; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wagons (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    wagon_number character varying(50) NOT NULL,
    wagon_type character varying(50) NOT NULL,
    max_weight_kg integer NOT NULL,
    max_volume_m3 integer NOT NULL,
    current_station character varying(255) NOT NULL,
    status character varying(50) DEFAULT 'свободен'::public.wagon_status_enum
);


ALTER TABLE public.wagons OWNER TO postgres;

--
-- Data for Name: cargo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cargo (id, order_id, cargo_type, weight_kg, volume_m3, packaging_type) FROM stdin;
ccdd3d50-a9ff-4928-b9c4-4f996f54131d	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	Электроника	60000	100	Паллеты
5253a648-eaf3-4146-a5bd-f62053a02675	5a0a1ea0-0466-464d-b792-8c66433e5605	Металл	60000	80	Паллеты
f18ed97d-4c85-4c5b-bf5c-81aeb36eaa92	4d2c5a42-50c3-40b3-8754-c208220083e6	Металл	60000	80	Паллеты
b7d64ed0-ad8e-407a-b592-50a71538f8b7	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	Металл	70000	40	Цистерна
3d8ec84a-1510-4f91-bbc7-3ea4fa7192b4	10dd8b55-f6c0-44ba-a385-a5929a839c5c	Электроника	1500	100	Паллеты
7ea3dd01-bbff-4f28-b794-61824dee4973	58c101db-544a-4e24-977d-d02f6e40648d	Электроника	1500	100	Паллеты
6109fb06-7fbb-4ed8-a205-8da4a506f3b7	001114fe-f612-4d78-8b76-37b3ea5dad64	Электроника	15000	1000	Паллеты
82715033-9315-4e79-aada-a949738b84bc	dd558da9-6be0-4637-985e-5a2b6735e50c	Электроника	1500	1000	Паллеты
b734e591-790e-4946-9ab5-5c1d650c2931	aeb4b302-f153-4c4b-80a3-a65597d5dadd	Электроника	1500	100	Паллеты
0981e46e-b55e-4ef3-ba77-7133f426fbbe	ace2fb72-b610-45ed-bdee-6335d84353d9	Электроника	1500	100	Паллеты
751a77b6-9c8b-4dc9-9e83-1755ec1f25a9	575d2f64-ee1d-463c-b84a-9a3dc25ebe11	Электроника	1500	100	Паллеты
e756f64d-e2a0-478e-bbba-3ce012e5911a	c2db69fe-e2c6-4de5-bb5b-1c2b30d90d92	Электроника	1500	100	Паллеты
d5ff8f89-b036-4685-b8d8-fb157e27bb25	43a3b781-f2f1-4d08-a339-2c6b466ee4fc	Электроника	1500	100	Паллеты
ba827062-b3f7-4810-8659-13551e110233	2a58481f-0b99-4bb8-9ed4-48c4aa3a2ae7	Электроника	150	100	Паллеты
ac437111-351f-4a4e-a1ae-26742157400f	a0518792-c911-456f-9966-2d7ab402fab2	Электроника	150	1000	Паллеты
0c40c3c4-ed1a-4475-af57-a1c1764374dd	ff146c7d-4473-4e5b-8437-dc67bbf7aed4	Электроника	150	100	Ящики
4db65376-2d00-4c0c-b6bf-cdaaa66c42ac	446e42d7-25f1-4e52-a58c-861527f08db3	Электроника	150	100	Ящики
3808400e-c450-4842-b914-9b9e2bdd4abf	f56b6404-8480-4c20-9fb8-ba5649e97489	Нефть	1500	1000	Ящики
74f24bae-6362-4749-aa30-3ba464b5a22a	1d1966ef-d2bf-43e0-995c-0999265e3777	Металл	1500	1000	Ящики
55588294-abb4-4479-a8a0-64da315e4a30	b19c9293-5850-4978-9943-1dab6d98d325	Металл	1500	1000	Ящики
42a4bbc7-bf22-4ebc-946f-7e164654e96b	65be754a-3728-4c54-b81a-9869395e638c	Металл	1500	100	Ящики
a08706bf-3b70-430f-ac68-f0c53e3d775d	8c89d5dc-44e2-41f4-ac6a-42cd324fd6cd	Уголь	1500	80	Ящики
a6eb63e8-c9cc-4d9b-8e5f-1926e6d5494b	363fb928-9ca0-48ed-b5b7-75dfcb691b76	Электроника	1500	80	Ящики
31637090-008a-476f-9076-85e4ae21a810	a99603bc-90bb-4dee-890e-2b1fcfbd6957	Электроника	150	80	Ящики
cbb9c121-5b8d-4d50-8a48-8d0e72c4c570	fd6ce5b1-361b-43f1-94c2-38f76a73e10d	Электроника	150	80	Паллеты
4c0c102f-36e1-4a81-b509-af2eb0985b11	78aacf59-bc25-4b61-845e-150a655d0ce2	Электроника	60000	80	Паллеты
a67c5480-7c22-44ee-8c96-ec749a58c163	c040ca0a-b480-4d6d-86b9-8378a911eb1b	Электроника	60000	100	Паллеты
2fdc7ac3-e6fb-42bb-9973-47dff0784ec2	d7b81c27-1f5d-43bc-b453-9b45ef9d1033	Электроника	60000	100	Паллеты
2995e115-4818-4c96-ae44-117b3c05635f	2efa532e-c146-4f1a-8043-b7ad0e96e83c	Электроника	60000	100	Паллеты
bbc38211-78a6-4af9-835b-2c23ebb52133	8715da53-20d9-487b-bd38-b9d62e2fe338	Электроника	60000	100	Паллеты
fc18ae46-aaa5-4e73-bd3f-ae1309466f60	ce34e9cb-21b6-4a46-a92a-fdbfbe0a113f	Электроника	60000	100	Паллеты
af8b7337-029c-4461-a4b4-4d93e93a6092	f5106d0c-65fe-48d1-a1c6-596608a14f71	Электроника	60000	100	Паллеты
0560e034-c148-47f6-95a7-429d90f3e6d5	78bf6919-58e0-4f25-b359-f82cc228646a	Электроника	60000	100	Паллеты
ed6cb285-795c-44a3-be2f-20ade6ce0751	6237939b-5d93-4e8e-a385-6a045ca1e263	Электроника	60000	100	Паллеты
18488f33-4e5b-4bee-bb74-de5854607f56	ec08374a-68cf-4757-b6f0-7823e28b2548	Электроника	15000	100	Паллеты
ae239e24-1d2c-4a99-a838-9cba5ee3fe5f	863d1b80-b65c-4fb8-9f42-4e277eb0b408	Электроника	15000	100	Паллеты
1fbc02a3-fba8-49de-bd3f-1ad319aa6ea8	f8771aad-2c79-4047-bd14-0906ed3704ce	Электроника	15000	100	Паллеты
fc518644-49da-4c20-928c-064ab8ddb0e5	c7ab5f5e-8438-4517-a2f4-3277b6958f71	Электроника	15000	100	Паллеты
17bb006b-3240-4b73-a34c-c8f3ab676250	7cdf26cb-f62c-4366-95f0-65e01801686f	Электроника	15000	100	Паллеты
9e0f7e82-6f24-47ec-a2b7-0d002c34f577	3824610a-e801-435e-b38e-05766c36d7b7	Электроника	15000	100	Паллеты
bff1e151-2f9c-4cc3-bae0-c1e3129a451c	36050184-4fc4-4fb9-9cc4-0d53f7977cf8	Электроника	15000	100	Паллеты
94d921b3-049f-4d40-9a0b-4cacdf6381c1	ce06cf6b-80a7-4a88-87d1-6a9a19843e55	Электроника	15000	100	Паллеты
1bd7d6ce-ccf1-4662-b341-7e78ebf9e990	8b0c4eff-c264-4231-97df-0bedb97893ed	Электроника	15000	100	Паллеты
7268f9af-903c-4b9f-bc58-48fa1a51a12f	917c8a13-d844-47a4-a2c0-605eeda3eb9d	Электроника	15000	100	Паллеты
8354b362-a572-4c4f-a10a-8a1e70d42e51	6cde2fe6-09f6-4401-91cf-d4ace7be4c8c	Электроника	15000	100	Паллеты
2b62125d-02e2-4732-9cff-eb9ddf03545d	7d23dab3-7028-4de5-89ee-d2cb66c754ad	Электроника	15000	100	Паллеты
a997932a-0d2e-42d4-8fec-cb3b8e5c660f	8bc23863-213e-4393-9554-d7a87c6b5ef0	Электроника	1500	100	Паллеты
8ee7c862-784f-430f-a236-1680528e0590	bb7e2aa1-abf2-48c8-afc4-8cb1981da977	Электроника	1500	100	Паллеты
65c344d0-fe12-4873-8c1b-0a1930028e18	e2522210-3b56-4289-bdd5-04fc205908b2	Электроника	1500	100	Паллеты
0ad71278-5395-409c-a336-f3bcd4120900	380dec2b-ea07-4a45-9e66-23a7f4e4cae7	Электроника	1500	100	Паллеты
36be71c2-73f6-4ab2-9f8c-2abd87bbd29f	7598fae7-53b4-4323-bb8d-69522b233990	Электроника	15000	100	Паллеты
a70690dc-0c2c-4afa-b0c3-263ed1c91f56	a3fc05b8-c18b-4bd2-8346-e7cc201285f3	Металл	15000	100	Паллеты
9be4c371-6702-4252-a7be-e2b5703feeac	6701e745-54d9-4672-9828-ff3c7b305a84	Металл	15000	100	Паллеты
838db879-9dfd-4eda-b006-d8d94895c663	6fea399f-a6a4-4518-92d7-72807eaca285	Металл	15000	100	Паллеты
8c752a3d-88c4-4a77-ba7e-832744b4f41e	3214c0fb-06c5-4447-9af1-adbb219a7f76	Металл	15000	100	Паллеты
da92416b-264c-44ac-b873-b4fe7f4c8858	ddb7bd5f-ba30-4022-a594-51f42a50424a	Уголь	65000	100	Паллеты
dfeffeae-b2c5-40b0-9e45-c10ff641485d	91c1e61e-c826-4a8d-a123-b6e4a408300a	Уголь	65000	100	Ящики
651c423b-a128-48d5-8eed-84a8b160ffbf	32873d18-f226-4481-a219-df02eac7cd4a	Уголь	65000	100	Паллеты
363213b7-b8b2-48b3-90f6-5b8e0dab710a	d2582536-da69-4fb5-96af-bdbe1b00c772	Уголь	65000	100	Паллеты
d3a8df1a-9100-4acf-8ba8-6d3e3af5362e	f4b2663d-b6b1-44b2-a2a8-57c836133685	Уголь	65000	80	Паллеты
09d2946a-92d4-40d3-a386-e072bf5c8525	86713774-c9d8-4532-aef3-82717accf5f6	Уголь	60000	80	Паллеты
e13f53b3-b5b2-41c1-840a-595e4f1439dc	6103bc12-aaac-42c7-8d5d-4d4a83d2cb66	Уголь	60000	100	Паллеты
6962ec6f-e25c-4618-8f2b-7231617890de	b2eed650-2ead-4085-b983-46d35e3b4a2f	Уголь	60000	100	Паллеты
104aa30e-22a9-431c-87cb-d67764b2a8c3	f7324323-e5f8-4fa1-b398-947e80ec6c60	Уголь	60000	100	Паллеты
f7595f5b-a431-451f-b445-dc0717d1113a	fddc22f3-131f-4c38-9b2f-4c575da6f68b	Электроника	60000	100	Паллеты
91af6120-997a-4c2e-bae0-6305cf78c1cb	5b6c1dd1-39be-43fc-8e41-bacc49b8ad0e	Электроника	60000	100	Паллеты
870c6142-e9bd-4874-8c0b-597516cbacf5	7fee1cbd-dfc4-404c-af26-83665e2ef67c	Металл	60000	100	Паллеты
0b75b0f0-ba02-418e-959a-7bb1ed78b0b9	926ef632-2b26-4df9-8349-2b2d1dc9387c	Металл	60000	100	Паллеты
2c0fb6fb-3263-4bc3-94be-a9cf2fcb1106	3f87e925-e08c-4e6e-bc63-1518735a4953	Металл	60000	100	Паллеты
fb595c01-4c80-4a42-8440-d9aa435658fb	894228f8-e3ae-455e-9b77-56e4c054e1fd	Металл	60000	80	Паллеты
57989643-8902-469c-8b5c-c88eccbcb4c2	d4c69684-d6b4-47f5-aa36-9bf72a09d37e	Металл	60000	80	Паллеты
dc44bb0a-7bb9-4221-ad87-770bb9375cb8	12c31283-83a8-4069-887e-6b3b3fcd7d4c	Уголь	60000	100	Паллеты
22dfc669-1530-4874-8fdf-b9e15f22fcc6	ad96aa7b-f72d-4ddd-9da6-88d298dc9fea	Оборудование	60000	100	Паллеты
db379868-fcf7-49c3-b5fc-63314c4bf720	100cb7a9-bd03-4e76-b5a7-6d7244b03387	Металл	60000	100	Паллеты
f6311b02-1b44-4767-9a62-87ed768c1ad1	4d5ead53-3fa8-4d73-82c6-2c3035f80866	Металл	60000	100	Паллеты
f680c95b-c8c6-4b5b-8073-babf856a1550	63597de4-db66-4838-806e-5434ad003f73	Нефть	60000	100	Паллеты
127e070e-f9df-4b91-ab43-311c7262541b	dca63373-e9c2-43ed-b1f8-6493bfe02695	Нефть	60000	100	Ящики
8c4be745-e984-49d3-a268-14fad2991c33	a8a88ac0-4622-4a63-b980-9adb319e20f7	Нефть	60000	100	Паллеты
67404a1e-ff15-4be5-ae1c-39e4ae837b27	e92d94cb-368f-436a-b971-2ffe8b22be7f	Нефть	60000	100	Паллеты
a405a9a1-8acf-4a15-a4fe-673cc1751c85	c03ea3a2-d321-45c8-be65-92ae0733b122	Уголь	60000	100	Паллеты
897b12ec-5f6d-45ed-8706-8555fcd0b4a8	28802d51-4c22-4145-a2d9-03ed5f276e9c	Электроника	10000	80	Паллеты
a0ac96c3-0397-4a8a-b1bf-f35573983068	8a8dbfa7-e82e-4828-8d36-a28f4a7b45b7	Лес	10000	80	Паллеты
a4856ada-7ca4-48ce-a72b-160a63468f06	e4f85c1e-ae86-4cad-93c7-562d03809c97	Лес	10000	80	Ящики
82863bbd-74b6-48cb-8293-495a0b419b93	3c6f53bb-f82d-4de1-9790-144108669c0a	Металл	10000	80	Ящики
781e3a61-9768-4b64-87a5-ead13d33dc7b	794e4f80-5df2-452e-9c08-3daf2f8ebdaf	Оборудование	10000	80	Ящики
894c5f9a-66ad-407c-9abe-4d709d914c94	42198c2c-0ee2-4722-9b47-d37800b8e7dd	Оборудование	10000	80	Ящики
0db6af73-a699-49d1-9ff4-d7ecdae66f0e	051d347f-2375-45c1-8c0c-c8148e6e074f	Оборудование	10000	80	Ящики
67b7a202-f647-4f47-b2be-0f558e062d5d	3187a3f3-8056-4803-b2b3-d4c0bbb2c0df	Оборудование	10000	80	Ящики
b3a8d399-ad7f-4612-b3b2-a8750ee7123c	93f7b7c4-8858-4915-8c3b-d3f61fcc64d4	Электроника	10000	100	Паллеты
67708370-c855-4cf3-a38c-226f767c66fc	106044e4-1eca-47e1-8389-c4fca1d78b8d	Электроника	10000	100	Паллеты
4a7d7cb6-befe-4787-bd16-9d7342baa8b8	bfbc2a9d-f509-45f1-8904-b44bbef41ebf	Электроника	10000	80	Паллеты
113df5f9-65ae-48c1-a9f1-d0a857992f43	745436ad-2d6a-4876-a6de-8568efe541b0	Электроника	10000	100	Паллеты
633723f5-30b2-45eb-a849-8973d79ea792	37107174-ea88-4302-9304-92cb4ec2d9d2	Электроника	10000	12	Паллеты
ecee062d-f4ba-4407-a69e-ecf2dfb2d5f2	59b10bf8-7f03-433c-8a19-6cec3255f096	Электроника	10000	12	Паллеты
366a638b-89c5-4e48-a14a-a16ed7384ec7	93330324-f86a-4110-87ea-7cb32bcea41a	Электроника	10000	12	Мешки
bfb963a6-bc3c-42a5-922c-bb375119502b	326a5002-b4ef-4f91-ba1c-4910fdd4c31a	Электроника	10000	12	Мешки
eda190f9-4bd1-478d-a3ec-61e66f41837c	86d6bbde-0b5e-48fc-b3ff-b3f332914996	Электроника	12313	12	Паллеты
d8c5fd53-9283-439b-a7af-7ee818d12446	2b705ea8-eaf4-4966-9066-03b945bdbc22	Электроника	12313	12	Паллеты
ff9a04c7-9470-4893-a61e-cd64dc6f5dec	3d507002-c7f6-4f3a-a95a-5d05c671378f	Электроника	10000	50	Паллеты
83f3bda3-d8dc-4f42-9f77-cd68fe623a01	cca64592-39ed-4e53-a84f-e003e4510a65	Электроника	10000	100	Паллеты
0391d9aa-0052-43e6-b1ad-540c957d5420	01a973bc-6112-4328-9b34-5e9c7e7d1791	Электроника	10000	100	Паллеты
aad33565-abea-49cd-98f5-c7bcd234a36b	ddd2c250-ada7-4161-8416-8969e13d7b22	Электроника	10000	100	Паллеты
fe06d1a1-0f49-4807-b10f-7ecef0a54afb	b0f90637-acf7-4a17-a88b-cbfe85e3e73c	Электроника	12030	100	Паллеты
211393bb-5cbc-4964-b930-140a0130d9d3	ccb41e6a-93ee-4aa1-9f2a-6064fdabd607	Электроника	10000	100	Паллеты
8baaeaf0-99ac-40a8-b8e7-40602d49c24e	eb21f47b-112c-48a4-8793-a576cb1fd512	Электроника	10000	100	Паллеты
89c61461-2c4f-49b6-9033-e92c0b4d7c1e	652abc8f-7d41-48d6-833e-2da051b4ca74	Электроника	10000	100	Паллеты
40ad4be3-4414-4b72-a4ab-fdd6842eb9be	0b1360ea-7f6b-4ee6-9a70-0326ae6894ce	Электроника	10000	100	Паллеты
0b93d45d-4276-45df-bb69-2baa028ac6fa	37ccb88f-6279-47fb-8b77-b565ba35b3bd	Электроника	10000	100	Паллеты
4ec75fd4-1c2f-431a-a2a2-1dd204c0a6d8	075ed92c-a7d4-4539-9637-df56638e06d6	Электроника	10000	100	Паллеты
447ba67c-12bf-4509-afd4-9bbd8b3d63e1	ef0d1dd7-ce2c-4ae9-801e-04c1e6cbb33a	Электроника	10000	100	Паллеты
c02ff7c0-4e92-49d0-a7d2-6ed933edf4e0	45ae095f-a304-46b5-aacd-53e9684a1d65	Электроника	10000	100	Паллеты
abc62cd9-4878-434c-a701-0a7c94b1e87d	4fd91b6e-ed4f-4df0-989b-5fd9dc506e66	Электроника	100	100	Паллеты
3b71539d-6ecd-4d98-8647-c89c2219055b	08846c7a-ca77-49bf-a889-20aac620d62d	Электроника	10000	100	Паллеты
9cbe3314-f52f-4dc7-88e0-773de76f5a6a	3fb5bf94-325e-4faf-b8ee-d6b86dc2ecd8	Электроника	10000	100	Паллеты
29cc1e47-a08a-452d-90c4-5ede4a4cefdc	864ad7f7-0c52-4870-b231-b7d88b439df7	Электроника	10000	100	Паллеты
7e265020-ea8c-44a4-80f2-9560645154ce	c6c9c178-096c-4ce2-a45b-ae1a098beb24	Электроника	100000	100	Паллеты
36b81f7f-028b-41f5-9501-a9b2b96ad0c3	30bc7f04-cbb7-4759-9293-2d151dd1f89f	Электроника	10000	100	Паллеты
a90d3ecd-7b81-4b5f-97dc-83a6aaa7393f	9002a813-e25b-4b9b-bc13-34f5d7db94cd	Электроника	10000	100	Паллеты
e094ded5-ab32-484b-9c8b-a1933d2f8a47	f9032e44-98f2-457c-af4a-657dd0504b1b	Электроника	10000	100	Паллеты
af51b3da-5080-452b-af12-aa357a4a131c	bf655024-71a3-4680-8c7a-c8d0497e4a38	Электроника	10000	100	Паллеты
4c962d6f-fc36-4307-ac42-b7f712d4be83	1eb4056d-c555-499d-9856-01017518fb90	Электроника	10000	100	Паллеты
ce49ca07-83c0-4777-94e7-8c817c81c4ea	a4c32e9f-cc37-45af-a0ed-d5ce900e575c	Электроника	10000	100	Паллеты
8c30a4b7-afb7-44bb-b1ad-4840ea9087ac	1c17dce3-8bce-48f4-a0e1-5a4ce02d5fb6	Электроника	10000	100	Паллеты
72a83003-b500-4282-993d-e6b07a7f8f0e	122d8026-a0be-4848-8960-c3a8d413cbff	Электроника	10000	100	Паллеты
61eb3793-1333-461e-a1f8-c5135b1cc446	8e6f3fe3-4f1f-4c69-86da-8ebc1b1ad13c	Электроника	10000	34	Паллеты
56e1b483-e27a-4685-b11c-e6194d4f9976	41d598e2-8442-4d0a-af06-568498479b0d	Электроника	10000	100	Паллеты
a2b5ebde-b104-4546-a23b-3751c06eca24	6e62691c-ac8d-49fe-b061-f54f61e7a08f	Электроника	10000	100	Паллеты
4ef0549e-be35-46f9-90be-6ed72bc8e4f8	ba29549e-83a1-4645-bba9-f6afb20572f9	Электроника	10000	100	Паллеты
6d710b06-9022-4902-bc56-dd0be7b552c9	24938503-1b08-41a7-a54b-9a4de5f7b21c	Электроника	10000	100	Паллеты
a764afff-5e1e-4e21-9322-8f5acb4bc32b	a626deca-2889-4f92-bccb-91f03db4b009	Электроника	10000	100	Паллеты
88d5258b-b15d-43b7-8598-2f9840cd932e	04b16866-24ce-486d-a463-f02df4e12fc4	Электроника	10000	100	Паллеты
eb934332-8e3d-4a28-b844-525da339f774	cf89be64-57ce-4365-9928-6cd1ab1ff826	Электроника	1000	100	Паллеты
4c12003d-95eb-4cea-8986-94cf93924fd4	2338f7ff-1a2d-4321-967f-cdaac335bda8	Электроника	1000	100	Паллеты
2f0a6024-e7b0-43bf-b9bd-ac827c2e2e6f	6ad3f8bf-e964-4926-8c0f-2024ca8b427b	Электроника	1000	100	Паллеты
ae438c81-55f4-472b-be8e-91c24186a4d1	450db10b-966f-45f1-a268-eead15a310a9	Электроника	1000	100	Паллеты
51f31080-1fd0-4376-b52b-d154f437cdb9	b8936df6-c39d-4179-9b2c-4ea3cb091c1e	Электроника	10000	100	Паллеты
c53fc6ab-c8fe-4a7a-8ffd-cc5ee335dab4	2d99a4f9-b3ad-4173-9954-c5491c746b1a	Электроника	10000	100	Паллеты
353960d4-8995-449e-a14c-501965b5b6c1	cd9c8f74-ab0e-4cff-b6fc-841804a631a6	Электроника	10000	100	Паллеты
8f73fbee-dab1-4cac-9680-61b621f60ceb	60500572-e65f-42f5-a437-07da062e61b8	Электроника	10000	100	Контейнеры
1200d13d-9ab6-4373-9409-d63809ebadd3	5b2128e3-b5cf-479b-b7f5-789726bd1ebc	Металл	10000	100	Контейнеры
985cf4cc-6f88-460e-beef-597d751f2627	4d4a327a-4e70-4f00-bddc-f7cb3ae2c7de	Металл	10000	100	Контейнеры
461cd4c7-1ed3-4e71-b9dc-76fa618353ca	4253788e-0048-412d-adf3-eed68f549218	Металл	10000	100	Паллеты
f9fca141-1519-4b53-ab75-78518b99f91f	8e3b9049-0c69-4350-af3e-30a527f92210	Электроника	10000	100	Паллеты
92454e97-a5f1-469a-ad64-a1e28670670f	bfe6cd17-b997-40bd-9050-73f1c1b9154b	Электроника	10000	100	Паллеты
4f206318-bc77-4766-802d-2cf0a106c70a	b2753090-3f1b-426e-9712-a2a4633738b5	Электроника	10000	100	Паллеты
4fcf87d5-1c04-4e15-a24b-d4460064d3e7	10a5e0fe-0c0f-455c-bd12-2c47f55ea00e	Металл	10000	100	Паллеты
6a444cb0-d954-459e-b9f5-2b80ea681450	8c33ed63-ccaf-44b8-9635-33f070eaf295	Лес	10000	100	Паллеты
4288759c-58bc-49d0-93dd-04b599c0d680	f03c6cf0-03a0-4ba3-9ad5-2d9ffd82f24d	Лес	10000	100	Паллеты
46b7df9e-1c8c-4fee-9084-6d9bbf5c3baf	118e84d3-3537-4b08-a09f-75ce9135641f	Лес	10000	100	Паллеты
7d9c9d74-5dad-459f-aad5-52a93dc5262a	fcf6581c-32bd-4290-b693-f8ca770059c1	Лес	10000	100	Паллеты
3060312e-f6d2-413f-b66b-ea35aaf28fd6	5b327642-63b4-4250-99b0-d3edce86f7d0	Электроника	10000	100	Паллеты
f2d614eb-fba9-4441-aaae-f08ffb21e5bd	d3f7eb05-e525-4dad-8850-41d691c78313	Электроника	10000	100	Паллеты
30496e3f-992b-4dae-b071-d72239f9dc2a	2931a42e-ed65-44da-be5c-1d73a4a7005c	Электроника	10000	100	Паллеты
38ffd563-22cf-4ff0-9902-8729df9c7013	9cfdbf9b-4fee-41b6-804d-d717059207ae	Электроника	10000	100	Паллеты
4939099a-146b-4d7d-b8a7-1faf1e041303	f769ca26-2248-4a81-b5e6-ae955e8054f7	Металл	10000	100	Паллеты
160531fd-803a-462c-b821-fa3155619e32	038e92ca-da8a-4b71-ac4d-db596cfd40e8	Электроника	10000	100	Паллеты
4f0be985-195e-4e3f-805a-41430a6b007a	a351ef92-43c7-4c9b-9b4e-a51517e5a592	Электроника	10000	100	Паллеты
6c61a898-1ce9-48bf-a277-4f11293f255b	fa54c358-75aa-4ebd-a349-d334d7c424c7	Электроника	10000	100	Паллеты
d24bd409-889e-459b-83ec-c83379671670	78a370aa-a2e6-4abc-8caf-f22b294a8fbd	Химия	60000	100	Паллеты
3240c8c0-05d5-41aa-b7d5-e3cda2e14e46	395010b4-8f35-4165-aa26-51ee3e524a4e	Химия	60000	100	Паллеты
cf4fdbae-4c1e-48f1-96eb-0442ea5f3cc6	41e9d66d-29de-4693-97e6-d2b1cbb6f76c	Электроника	19999	100	Паллеты
\.


--
-- Data for Name: company_accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company_accounts (id, inn, company_name, account_number, balance, bik, bank_name, created_at, updated_at, is_main, is_rzd_account) FROM stdin;
8f22f052-a323-4518-8c45-1f3380d7bf39	231231321	ООО СОМ	40702810231231327476	500000.00	044525225	ПАО СБЕРБАНК	2026-02-27 18:26:57.83126+03	2026-02-27 18:26:57.83126+03	f	f
34c06c0f-b6e8-4eb8-b667-6b719f03222d	7701234567	ООО Ромашка	40702810770123451761	199998.00	044525225	ПАО СБЕРБАНК	2026-02-27 17:56:54.705452+03	2026-03-01 16:55:46.465772+03	f	f
e57bdb89-6e7e-4e3e-ad15-2c658776550d	7701234567	ООО Вектор	40702810123450000001	500000.00	044525225	ПАО СБЕРБАНК	2026-03-01 17:40:57.358288+03	2026-03-01 17:40:57.358288+03	t	f
0879c817-c1dd-4825-8d4e-cc6f1d2de049	7701234567	ООО Вектор	40702810765430000002	250000.00	044525111	АО АЛЬФА-БАНК	2026-03-01 17:40:57.358288+03	2026-03-01 17:40:57.358288+03	f	f
ffcf3419-5617-4ed6-b6a6-b3895495dfa8	7701234567	ООО Вектор	40702810987650000003	100000.00	044525593	ПАО ВТБ	2026-03-01 17:40:57.358288+03	2026-03-01 17:40:57.358288+03	f	f
3654427e-3752-41ed-b7fc-9447aa99a1a9	7709876543	ЗАО Ромашка	40702810234560000004	750000.00	044525225	ПАО СБЕРБАНК	2026-03-01 17:40:58.14747+03	2026-03-01 17:40:58.14747+03	t	f
e4379784-e225-4ab8-808b-c25e549da7cb	7709876543	ЗАО Ромашка	40702810765430000005	120000.00	044525111	АО АЛЬФА-БАНК	2026-03-01 17:40:58.14747+03	2026-03-01 17:40:58.14747+03	f	f
47e227be-a3f4-4f20-bd67-ce9474db2077	1234512345	ООО Абас	40702810123451230298	48399280.00	044525225	ПАО СБЕРБАНК	2026-03-01 17:47:30.977856+03	2026-03-01 17:47:30.977856+03	t	f
1eaaa54f-6bdd-42e4-86c1-48d62d0f0ed4	7708503727	ОАО РЖД (Грузовые перевозки)	40702810900000000001	12200722.00	044525225	ПАО СБЕРБАНК	2026-02-27 17:51:33.334983+03	2026-03-01 17:40:41.13943+03	t	t
\.


--
-- Data for Name: order_services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_services (id, order_id, service_name, price) FROM stdin;
b1244148-2e88-44ed-a418-0b0c058d9e0e	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	страхование	5000.00
6c6fa768-7065-48df-b412-5d440b7883ad	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	сопровождение	15000.00
3d9af5be-d84d-49b2-8412-0ba939053d81	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	терминальная_обработка	8000.00
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orders (id, user_id, departure_station, destination_station, wagon_id, status, total_price, carbon_footprint_kg, created_at, requested_wagon_type) FROM stdin;
c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11	Москва-Товарная	Екатеринбург	\N	поиск_вагона	\N	\N	2026-02-26 19:44:19.318287+03	КРЫТЫЙ
d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22	Санкт-Петербург	Новосибирск	44444444-4444-4444-4444-444444444444	ожидает_оплаты	150000.00	450.50	2026-02-26 19:44:19.41476+03	КРЫТЫЙ
10dd8b55-f6c0-44ba-a385-a5929a839c5c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:04:33.817414+03	крытый
58c101db-544a-4e24-977d-d02f6e40648d	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:05:47.852391+03	крытый
001114fe-f612-4d78-8b76-37b3ea5dad64	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:31:35.661132+03	крытый
dd558da9-6be0-4637-985e-5a2b6735e50c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:31:39.206314+03	крытый
aeb4b302-f153-4c4b-80a3-a65597d5dadd	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:31:41.889831+03	крытый
ace2fb72-b610-45ed-bdee-6335d84353d9	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:35:47.580305+03	крытый
575d2f64-ee1d-463c-b84a-9a3dc25ebe11	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:44:24.077624+03	крытый
c2db69fe-e2c6-4de5-bb5b-1c2b30d90d92	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:04.062596+03	крытый
43a3b781-f2f1-4d08-a339-2c6b466ee4fc	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:13.991245+03	полувагон
2a58481f-0b99-4bb8-9ed4-48c4aa3a2ae7	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:19.044148+03	полувагон
a0518792-c911-456f-9966-2d7ab402fab2	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:21.236976+03	полувагон
ff146c7d-4473-4e5b-8437-dc67bbf7aed4	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:53.095928+03	полувагон
446e42d7-25f1-4e52-a58c-861527f08db3	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:47:55.739014+03	крытый
f56b6404-8480-4c20-9fb8-ba5649e97489	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:48:12.992812+03	крытый
1d1966ef-d2bf-43e0-995c-0999265e3777	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:48:16.010985+03	крытый
b19c9293-5850-4978-9943-1dab6d98d325	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:51:34.699932+03	крытый
65be754a-3728-4c54-b81a-9869395e638c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:51:38.097347+03	крытый
8c89d5dc-44e2-41f4-ac6a-42cd324fd6cd	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:53:01.325343+03	крытый
363fb928-9ca0-48ed-b5b7-75dfcb691b76	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:53:21.65484+03	крытый
a99603bc-90bb-4dee-890e-2b1fcfbd6957	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:53:34.337946+03	крытый
fd6ce5b1-361b-43f1-94c2-38f76a73e10d	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:54:49.116183+03	крытый
78aacf59-bc25-4b61-845e-150a655d0ce2	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:55:14.840722+03	крытый
c040ca0a-b480-4d6d-86b9-8378a911eb1b	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:55:25.097625+03	крытый
d7b81c27-1f5d-43bc-b453-9b45ef9d1033	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:55:57.119881+03	крытый
2efa532e-c146-4f1a-8043-b7ad0e96e83c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:55:59.407407+03	крытый
8715da53-20d9-487b-bd38-b9d62e2fe338	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:55:59.95922+03	крытый
ce34e9cb-21b6-4a46-a92a-fdbfbe0a113f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:56:00.672127+03	крытый
f5106d0c-65fe-48d1-a1c6-596608a14f71	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:56:00.976902+03	крытый
78bf6919-58e0-4f25-b359-f82cc228646a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:56:01.094004+03	крытый
6237939b-5d93-4e8e-a385-6a045ca1e263	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:56:01.453773+03	крытый
ec08374a-68cf-4757-b6f0-7823e28b2548	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:56:45.525124+03	крытый
863d1b80-b65c-4fb8-9f42-4e277eb0b408	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:58:59.936443+03	крытый
f8771aad-2c79-4047-bd14-0906ed3704ce	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:02.562379+03	крытый
c7ab5f5e-8438-4517-a2f4-3277b6958f71	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:03.109454+03	крытый
7cdf26cb-f62c-4366-95f0-65e01801686f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:03.701875+03	крытый
3824610a-e801-435e-b38e-05766c36d7b7	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:04.250413+03	крытый
36050184-4fc4-4fb9-9cc4-0d53f7977cf8	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:04.778366+03	крытый
ce06cf6b-80a7-4a88-87d1-6a9a19843e55	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:05.387193+03	крытый
8b0c4eff-c264-4231-97df-0bedb97893ed	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 13:59:05.792228+03	крытый
917c8a13-d844-47a4-a2c0-605eeda3eb9d	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:01:44.426111+03	крытый
6cde2fe6-09f6-4401-91cf-d4ace7be4c8c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:04:00.843247+03	крытый
7d23dab3-7028-4de5-89ee-d2cb66c754ad	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:08:46.745295+03	крытый
8bc23863-213e-4393-9554-d7a87c6b5ef0	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:16:26.60344+03	крытый
bb7e2aa1-abf2-48c8-afc4-8cb1981da977	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:19:23.73112+03	крытый
e2522210-3b56-4289-bdd5-04fc205908b2	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:22:22.582597+03	крытый
380dec2b-ea07-4a45-9e66-23a7f4e4cae7	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:33:06.377362+03	крытый
7598fae7-53b4-4323-bb8d-69522b233990	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:33:10.402409+03	крытый
a3fc05b8-c18b-4bd2-8346-e7cc201285f3	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:33:31.054245+03	крытый
6701e745-54d9-4672-9828-ff3c7b305a84	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:33:35.942138+03	полувагон
6fea399f-a6a4-4518-92d7-72807eaca285	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:33:39.318355+03	платформа
3214c0fb-06c5-4447-9af1-adbb219a7f76	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:44:55.360245+03	платформа
ddb7bd5f-ba30-4022-a594-51f42a50424a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:45:13.722716+03	полувагон
91c1e61e-c826-4a8d-a123-b6e4a408300a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:45:17.275688+03	полувагон
32873d18-f226-4481-a219-df02eac7cd4a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 14:45:24.977528+03	крытый
d2582536-da69-4fb5-96af-bdbe1b00c772	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 15:08:06.24389+03	крытый
f4b2663d-b6b1-44b2-a2a8-57c836133685	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 15:09:38.998599+03	крытый
86713774-c9d8-4532-aef3-82717accf5f6	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург	\N	черновик	\N	\N	2026-02-28 15:12:58.52027+03	крытый
6103bc12-aaac-42c7-8d5d-4d4a83d2cb66	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 16:47:01.143539+03	крытый
b2eed650-2ead-4085-b983-46d35e3b4a2f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 16:50:32.012376+03	крытый
f7324323-e5f8-4fa1-b398-947e80ec6c60	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:06:25.758716+03	крытый
fddc22f3-131f-4c38-9b2f-4c575da6f68b	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:07:23.845261+03	крытый
5b6c1dd1-39be-43fc-8e41-bacc49b8ad0e	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург	\N	черновик	\N	\N	2026-02-28 17:14:23.892443+03	крытый
7fee1cbd-dfc4-404c-af26-83665e2ef67c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург	\N	черновик	\N	\N	2026-02-28 17:14:28.07087+03	крытый
926ef632-2b26-4df9-8349-2b2d1dc9387c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург	\N	черновик	\N	\N	2026-02-28 17:14:34.308055+03	цистерна
3f87e925-e08c-4e6e-bc63-1518735a4953	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:14:42.659834+03	цистерна
894228f8-e3ae-455e-9b77-56e4c054e1fd	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:14:46.399452+03	цистерна
5a0a1ea0-0466-464d-b792-8c66433e5605	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:20:00.994452+03	цистерна
4d2c5a42-50c3-40b3-8754-c208220083e6	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:20:08.251654+03	цистерна
d4c69684-d6b4-47f5-aa36-9bf72a09d37e	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:20:15.537156+03	цистерна
12c31283-83a8-4069-887e-6b3b3fcd7d4c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:03.432399+03	крытый
ad96aa7b-f72d-4ddd-9da6-88d298dc9fea	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:07.160461+03	крытый
100cb7a9-bd03-4e76-b5a7-6d7244b03387	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:10.845299+03	крытый
4d5ead53-3fa8-4d73-82c6-2c3035f80866	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:15.582989+03	цистерна
63597de4-db66-4838-806e-5434ad003f73	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:19.622566+03	цистерна
dca63373-e9c2-43ed-b1f8-6493bfe02695	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:22.513115+03	цистерна
a8a88ac0-4622-4a63-b980-9adb319e20f7	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:28:25.810368+03	полувагон
e92d94cb-368f-436a-b971-2ffe8b22be7f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:34:05.455964+03	полувагон
c03ea3a2-d321-45c8-be65-92ae0733b122	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 17:36:26.303141+03	крытый
28802d51-4c22-4145-a2d9-03ed5f276e9c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:12.278984+03	крытый
8a8dbfa7-e82e-4828-8d36-a28f4a7b45b7	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:15.924552+03	крытый
e4f85c1e-ae86-4cad-93c7-562d03809c97	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:18.787305+03	крытый
3c6f53bb-f82d-4de1-9790-144108669c0a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:24.963488+03	крытый
794e4f80-5df2-452e-9c08-3daf2f8ebdaf	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:27.704612+03	крытый
42198c2c-0ee2-4722-9b47-d37800b8e7dd	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:33.193057+03	крытый
051d347f-2375-45c1-8c0c-c8148e6e074f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:06:36.937331+03	крытый
3187a3f3-8056-4803-b2b3-d4c0bbb2c0df	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:07:39.400454+03	полувагон
93f7b7c4-8858-4915-8c3b-d3f61fcc64d4	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:40:24.181757+03	крытый
106044e4-1eca-47e1-8389-c4fca1d78b8d	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:40:35.389542+03	полувагон
bfbc2a9d-f509-45f1-8904-b44bbef41ebf	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:40:49.819595+03	полувагон
745436ad-2d6a-4876-a6de-8568efe541b0	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Санкт-Петербург	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-02-28 18:57:05.084582+03	крытый
37107174-ea88-4302-9304-92cb4ec2d9d2	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Москва	\N	черновик	\N	\N	2026-02-28 18:59:47.147355+03	крытый
59b10bf8-7f03-433c-8a19-6cec3255f096	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Москва	\N	черновик	\N	\N	2026-02-28 18:59:49.86229+03	цистерна
93330324-f86a-4110-87ea-7cb32bcea41a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Казань	Москва	\N	черновик	\N	\N	2026-02-28 18:59:52.500357+03	цистерна
326a5002-b4ef-4f91-ba1c-4910fdd4c31a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Казань	\N	черновик	\N	\N	2026-02-28 19:00:13.177003+03	полувагон
86d6bbde-0b5e-48fc-b3ff-b3f332914996	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	два	ыва	\N	черновик	\N	\N	2026-02-28 19:00:58.801455+03	крытый
2b705ea8-eaf4-4966-9066-03b945bdbc22	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	два	ыва	\N	черновик	\N	\N	2026-02-28 19:01:14.134221+03	крытый
3d507002-c7f6-4f3a-a95a-5d05c671378f	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Казань	\N	черновик	\N	\N	2026-02-28 19:39:43.763388+03	полувагон
cca64592-39ed-4e53-a84f-e003e4510a65	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва-Товарная	Казань	\N	черновик	\N	\N	2026-02-28 19:52:36.562048+03	рефрижератор
01a973bc-6112-4328-9b34-5e9c7e7d1791	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Красноярск	Казань	\N	черновик	\N	\N	2026-02-28 20:35:53.877091+03	крытый
ddd2c250-ada7-4161-8416-8969e13d7b22	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Челябинск	Казань	\N	черновик	\N	\N	2026-02-28 20:47:55.623282+03	полувагон
b0f90637-acf7-4a17-a88b-cbfe85e3e73c	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Владивосток	Казань	\N	черновик	\N	\N	2026-02-28 20:51:38.603562+03	рефрижератор
ccb41e6a-93ee-4aa1-9f2a-6064fdabd607	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Москва	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:48:30.456973+03	крытый
eb21f47b-112c-48a4-8793-a576cb1fd512	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Санкт-Петерубрг	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:48:40.456035+03	цистерна
652abc8f-7d41-48d6-833e-2da051b4ca74	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Санкт-Петерубрг	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:48:42.361931+03	полувагон
0b1360ea-7f6b-4ee6-9a70-0326ae6894ce	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Новосибирск	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:48:48.705548+03	полувагон
37ccb88f-6279-47fb-8b77-b565ba35b3bd	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Новосибирск	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:48:51.015716+03	крытый
075ed92c-a7d4-4539-9637-df56638e06d6	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Омск	Екатеринбург	\N	черновик	\N	\N	2026-03-01 15:49:23.890231+03	полувагон
ef0d1dd7-ce2c-4ae9-801e-04c1e6cbb33a	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Омск	Москва	\N	черновик	\N	\N	2026-03-01 16:06:28.895088+03	рефрижератор
45ae095f-a304-46b5-aacd-53e9684a1d65	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Уфа	Москва	\N	черновик	\N	\N	2026-03-01 16:06:39.120815+03	рефрижератор
4fd91b6e-ed4f-4df0-989b-5fd9dc506e66	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Воронеж	Москва	\N	поиск_вагона	\N	\N	2026-03-01 16:28:47.509262+03	крытый
08846c7a-ca77-49bf-a889-20aac620d62d	0f22dba8-eb37-460f-acd2-f1fe8ec324e3	Самара	Москва	\N	поиск_вагона	\N	\N	2026-03-01 16:49:34.067642+03	платформа
1c17dce3-8bce-48f4-a0e1-5a4ce02d5fb6	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Москва	\N	поиск_вагона	\N	\N	2026-03-01 18:27:46.536029+03	крытый
3fb5bf94-325e-4faf-b8ee-d6b86dc2ecd8	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Москва	\N	оплачен	\N	\N	2026-03-01 17:48:17.916288+03	рефрижератор
864ad7f7-0c52-4870-b231-b7d88b439df7	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Пермь	Москва	\N	поиск_вагона	\N	\N	2026-03-01 18:03:32.709501+03	платформа
c6c9c178-096c-4ce2-a45b-ae1a098beb24	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Екатеринбург	Москва	\N	черновик	\N	\N	2026-03-01 18:08:05.445815+03	платформа
30bc7f04-cbb7-4759-9293-2d151dd1f89f	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Екатеринбург	Москва	\N	оплачен	\N	\N	2026-03-01 18:08:15.932437+03	платформа
122d8026-a0be-4848-8960-c3a8d413cbff	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Санкт-Петербург	\N	поиск_вагона	\N	\N	2026-03-01 18:30:12.869986+03	крытый
9002a813-e25b-4b9b-bc13-34f5d7db94cd	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск	Москва	\N	оплачен	\N	\N	2026-03-01 18:17:10.480993+03	цистерна
f9032e44-98f2-457c-af4a-657dd0504b1b	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Нижний Новогород	Москва	\N	черновик	\N	\N	2026-03-01 18:20:16.278141+03	крытый
bf655024-71a3-4680-8c7a-c8d0497e4a38	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск	Москва	\N	черновик	\N	\N	2026-03-01 18:20:27.955192+03	крытый
1eb4056d-c555-499d-9856-01017518fb90	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск	Москва	\N	черновик	\N	\N	2026-03-01 18:20:33.469954+03	цистерна
a4c32e9f-cc37-45af-a0ed-d5ce900e575c	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Ростов-на-Дону	Москва	\N	поиск_вагона	\N	\N	2026-03-01 18:20:49.136358+03	цистерна
8e6f3fe3-4f1f-4c69-86da-8ebc1b1ad13c	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Санкт-Петербург	\N	поиск_вагона	\N	\N	2026-03-01 18:36:07.37544+03	цистерна
41d598e2-8442-4d0a-af06-568498479b0d	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Москва	\N	поиск_вагона	\N	\N	2026-03-01 18:42:35.006218+03	крытый
6e62691c-ac8d-49fe-b061-f54f61e7a08f	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Уфа	\N	поиск_вагона	\N	\N	2026-03-01 18:46:12.463583+03	крытый
04b16866-24ce-486d-a463-f02df4e12fc4	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Уфа	11111111-1111-1111-1111-345453522322	оплачен	191000.00	\N	2026-03-01 19:07:34.035105+03	крытый
24938503-1b08-41a7-a54b-9a4de5f7b21c	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Москва	22222222-2222-2222-2222-789789644535	оплачен	191000.00	\N	2026-03-01 18:59:18.213011+03	крытый
ba29549e-83a1-4645-bba9-f6afb20572f9	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Уфа	11111111-1111-1111-1111-756757576756	оплачен	191000.00	\N	2026-03-01 18:49:54.632734+03	крытый
a626deca-2889-4f92-bccb-91f03db4b009	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Москва	22222222-2222-2222-2222-213124354656	оплачен	191000.00	\N	2026-03-01 19:03:19.355947+03	крытый
2338f7ff-1a2d-4321-967f-cdaac335bda8	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск-Товарный	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:03:15.848444+03	крытый
cf89be64-57ce-4365-9928-6cd1ab1ff826	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск-Товарный	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:03:06.024204+03	крытый
6ad3f8bf-e964-4926-8c0f-2024ca8b427b	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Нижний Новгород-Товарный	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:03:53.485937+03	крытый
450db10b-966f-45f1-a268-eead15a310a9	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Нижний Новгород-Товарный	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:04:29.502787+03	крытый
b8936df6-c39d-4179-9b2c-4ea3cb091c1e	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Нижний Новгород-Товарный	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:04:36.399557+03	крытый
2d99a4f9-b3ad-4173-9954-c5491c746b1a	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Нижний Новгород	Екатеринбург-Товарный	\N	черновик	\N	\N	2026-03-01 21:04:55.300399+03	крытый
cd9c8f74-ab0e-4cff-b6fc-841804a631a6	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Пермь	Екатеринбург	\N	черновик	\N	\N	2026-03-02 13:47:22.254654+03	крытый
60500572-e65f-42f5-a437-07da062e61b8	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Пермь	Екатеринбург	\N	черновик	\N	\N	2026-03-02 13:47:37.031453+03	полувагон
5b2128e3-b5cf-479b-b7f5-789726bd1ebc	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Пермь	Екатеринбург	\N	черновик	\N	\N	2026-03-02 13:47:43.161797+03	полувагон
4d4a327a-4e70-4f00-bddc-f7cb3ae2c7de	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Екатеринбург	\N	черновик	\N	\N	2026-03-02 13:47:48.808999+03	полувагон
4253788e-0048-412d-adf3-eed68f549218	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Екатеринбург	\N	черновик	\N	\N	2026-03-02 13:48:56.892409+03	крытый
8e3b9049-0c69-4350-af3e-30a527f92210	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	\N	черновик	\N	\N	2026-03-02 17:03:27.872339+03	крытый
bfe6cd17-b997-40bd-9050-73f1c1b9154b	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Санкт-Петербург	Уфа	\N	черновик	\N	\N	2026-03-02 17:04:09.255342+03	крытый
b2753090-3f1b-426e-9712-a2a4633738b5	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Самара	Екатеринбург	\N	черновик	\N	\N	2026-03-02 17:07:03.124476+03	крытый
10a5e0fe-0c0f-455c-bd12-2c47f55ea00e	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск	Екатеринбург	\N	черновик	\N	\N	2026-03-02 17:07:13.978526+03	полувагон
8c33ed63-ccaf-44b8-9635-33f070eaf295	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Новосибирск	Екатеринбург	\N	черновик	\N	\N	2026-03-02 17:07:17.139446+03	полувагон
f03c6cf0-03a0-4ba3-9ad5-2d9ffd82f24d	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Екатеринбург	\N	черновик	\N	\N	2026-03-02 17:07:24.352694+03	полувагон
118e84d3-3537-4b08-a09f-75ce9135641f	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Екатеринбург	bad79eb0-3c4f-45cf-be5e-22ef2d710159	ожидает_оплаты	178000.00	\N	2026-03-02 17:07:26.884146+03	крытый
fcf6581c-32bd-4290-b693-f8ca770059c1	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Уфа	Екатеринбург	22222222-2222-2222-2222-333333333333	ожидает_оплаты	163000.00	\N	2026-03-02 17:19:55.375764+03	крытый
5b327642-63b4-4250-99b0-d3edce86f7d0	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	11111111-1111-1111-1111-111111111111	оплачен	241000.00	\N	2026-03-02 17:26:11.489729+03	крытый
d3f7eb05-e525-4dad-8850-41d691c78313	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва	Екатеринбург	11111111-1111-1111-1111-454545454545	оплачен	202860.00	\N	2026-03-02 17:40:43.371825+03	крытый
2931a42e-ed65-44da-be5c-1d73a4a7005c	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	11111111-1111-1111-1111-111111111111	ожидает_оплаты	232860.00	\N	2026-03-02 17:47:42.504719+03	крытый
9cfdbf9b-4fee-41b6-804d-d717059207ae	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	11111111-1111-1111-1111-111111111111	ожидает_оплаты	191000.00	\N	2026-03-02 17:48:37.769328+03	крытый
f769ca26-2248-4a81-b5e6-ae955e8054f7	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	11111111-1111-1111-1111-111111111111	ожидает_оплаты	137000.00	\N	2026-03-02 17:54:16.786773+03	крытый
038e92ca-da8a-4b71-ac4d-db596cfd40e8	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	11111111-1111-1111-1111-111111111111	оплачен	242860.00	\N	2026-03-02 18:01:14.576264+03	крытый
a351ef92-43c7-4c9b-9b4e-a51517e5a592	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	\N	черновик	\N	\N	2026-03-03 10:29:57.709611+03	крытый
fa54c358-75aa-4ebd-a349-d334d7c424c7	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	\N	черновик	\N	\N	2026-03-03 10:31:18.602594+03	крытый
78a370aa-a2e6-4abc-8caf-f22b294a8fbd	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Уфа	\N	черновик	\N	\N	2026-03-03 10:32:25.236705+03	рефрижератор
395010b4-8f35-4165-aa26-51ee3e524a4e	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	Москва-Товарная	Екатеринбург	\N	черновик	\N	\N	2026-03-03 10:40:40.717403+03	рефрижератор
41e9d66d-29de-4693-97e6-d2b1cbb6f76c	7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	москва	москва	\N	черновик	\N	\N	2026-03-03 10:58:48.824508+03	крытый
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (id, order_id, payment_id, amount, status, payment_method, company_name, inn, kpp, bik, account_number, correspondent_account, bank_name, payment_purpose, payment_document, payment_date, created_at, paid_at, error_message, metadata) FROM stdin;
1cad2062-9561-45a7-806a-11a1fcc0ec8c	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	pay_123456789	150000.00	SUCCEEDED	BANK_TRANSFER	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	\N	ПАО СБЕРБАНК	Оплата грузовой перевозки по договору №РЖД-2026-123	РЖД-1712345678-123	\N	2026-02-27 10:00:00+03	\N	\N	\N
d68b2a7b-b167-45ab-9e62-2f1b3297f258	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	pay_987654321	75000.00	PENDING	BANK_TRANSFER	ООО Вектор	7709876543	770901001	044525111	40702810567890123456	\N	АО АЛЬФА-БАНК	Предоплата за перевозку оборудования	РЖД-1712345678-124	\N	2026-02-27 11:30:00+03	\N	\N	\N
81d463b7-3feb-4b8c-98fa-6184e8c90b66	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	15001.00	PENDING	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772193923-550	\N	2026-02-27 15:05:23.639676+03	\N	\N	\N
ac959ead-2844-4572-9fee-8d5f278223b4	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	1500121.00	PENDING	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772193994-734	\N	2026-02-27 15:06:34.846007+03	\N	\N	\N
a059c5d7-256b-4383-9c9e-cca89cd8cad5	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	15003221.00	PENDING	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772194018-088	\N	2026-02-27 15:06:58.877608+03	\N	\N	\N
ced450eb-5d7e-4630-8d82-a8b0508631ca	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	1500221.00	PENDING	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772194155-150	\N	2026-02-27 15:09:15.468497+03	\N	\N	\N
5f0f8f46-e462-4818-86de-f70408f7329b	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	1231312.00	PENDING	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772194228-884	\N	2026-02-27 15:10:28.557356+03	\N	\N	\N
6a4005b0-bf56-489e-9d5f-540b01f71fd9	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	bank_987654321_12345	150000.00	SUCCEEDED	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	ПП-123456	2026-02-27 15:30:00+03	2026-02-27 14:58:50.155075+03	2026-02-27 15:15:17.934504+03	\N	\N
1f8ac1d7-3438-4069-afd8-fba9d6c78f49	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	\N	1231312.00	PENDING	\N	ООО Помашка	7701234569	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	РЖД-1772194898-416	\N	2026-02-27 15:21:38.741794+03	\N	\N	\N
1f05f549-ae04-4160-8873-1fba07219ee4	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	bank_123456789	12232.00	SUCCEEDED	\N	ООО Помашка	7701234567	770101001	044525225	40702810123450123456	\N	ПАО СБЕРБАНК	Оплата грузовой перевозки	ПП-123456	\N	2026-02-27 15:28:07.538226+03	2026-02-27 15:28:50.045704+03	\N	\N
c2c7d0cc-525a-4688-8887-b3674bee9588	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	bank_{{ $timestamp }}	150000.00	SUCCEEDED	\N	ООО Ромашка	7701234597	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	ПП-{{ $timestamp }}	2026-02-27 15:38:55.111+03	2026-02-27 15:38:47.107501+03	2026-02-27 15:38:55.15697+03	\N	\N
46b9abc4-d02c-4ede-872f-4aa8a948edc5	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	bank_real_1772204493661	150001.00	SUCCEEDED	\N	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	30101810400000000225	ПАО СБЕРБАНК	Оплата грузовой перевозки	ПП-1772204493661	2026-02-27 18:01:33.662006+03	2026-02-27 18:00:04.95433+03	2026-02-27 18:01:33.674554+03	\N	\N
8a02e209-1db9-4d02-b599-04bcb9e611f7	fddc22f3-131f-4c38-9b2f-4c575da6f68b	\N	150000.00	PENDING	\N	ООО Ромашка	1234567891	123456789	123456789	40702810123450123456		ПАО Сбербанк	оплата	РЖД-1772287714-778	\N	2026-02-28 17:08:34.030798+03	\N	\N	\N
2bcc000b-e795-4991-8505-fcadec09e707	bfbc2a9d-f509-45f1-8904-b44bbef41ebf	\N	150000.00	PENDING	\N	ООО Помидор	111111111111	111111111	231231231	11111111111111111111		ПАО ВТБ	Оплата 	РЖД-1772293337-359	\N	2026-02-28 18:42:17.658005+03	\N	\N	\N
95e0a5ed-50ad-44c4-80dd-28e36b6552c9	745436ad-2d6a-4876-a6de-8568efe541b0	\N	150000.00	PENDING	\N	ООО Инстед	1231231221	123123123	123123123	12345123451234512345		ПАО Альфа банк	Оплата	РЖД-1772294290-724	\N	2026-02-28 18:58:10.071637+03	\N	\N	\N
66b20556-9c02-4e9a-82e9-28b5411a27ed	3d507002-c7f6-4f3a-a95a-5d05c671378f	\N	150000.00	PENDING	\N	ООО Интернешенл	1231231123	\N	123123123	12345123451234512345		ПАО Сбербанк	Оплата услуг 	РЖД-1772296845-894	\N	2026-02-28 19:40:45.027081+03	\N	\N	\N
1dd6e4ed-6e07-4971-8225-ed7b405eec7a	3d507002-c7f6-4f3a-a95a-5d05c671378f	\N	150000.00	PENDING	\N	ООО Идея	1234567891	\N	123456789	12345678912345678911		ПАО сбер 	Оплата	РЖД-1772297232-676	\N	2026-02-28 19:47:12.355611+03	\N	\N	\N
0585e040-b56d-4853-8a7b-7423f475769b	cca64592-39ed-4e53-a84f-e003e4510a65	\N	150000.00	PENDING	\N	ООО Роман	1231231231	\N	123123123	12312312311231231231		а	а	РЖД-1772297585-332	\N	2026-02-28 19:53:05.595007+03	\N	\N	\N
1e07dd93-1e1e-4fbb-8f1c-0489cccfa87d	01a973bc-6112-4328-9b34-5e9c7e7d1791	\N	150000.00	PENDING	\N	ООО СОМ	231231321	\N	123123123	12345671231234567123		ПАО Сбер	оплата груза	РЖД-1772300214-045	\N	2026-02-28 20:36:54.684506+03	\N	\N	\N
9c28ebc8-ca60-4288-b10f-220afe743729	b0f90637-acf7-4a17-a88b-cbfe85e3e73c	\N	150000.00	PENDING	\N	ООО СОМ	1234567890	\N	123123123	12312312312312312312		ПАО Сбербанк	Оплата лесоматериалов	РЖД-1772301126-058	\N	2026-02-28 20:52:06.288252+03	\N	\N	\N
7764f5da-7f1a-4e83-b9f6-27147d96f399	075ed92c-a7d4-4539-9637-df56638e06d6	\N	150000.00	PENDING	\N	ООО СОМ	1234567890	123456789	123456789	12345678912345678911		ПАО Сбербанк 	Оплата очень важного груза	РЖД-1772369401-140	\N	2026-03-01 15:50:01.281537+03	\N	\N	\N
3e257a10-083c-4d65-aacf-15fafc83555d	4fd91b6e-ed4f-4df0-989b-5fd9dc506e66	\N	150000.00	PENDING	\N	ООО СОМ	1234567890	\N	123123123	12312312312312312311		ПАО Сбербанк	Оплата груза 	РЖД-1772371762-294	\N	2026-03-01 16:29:22.476849+03	\N	\N	\N
ed05dd2f-0495-465c-a26b-933280d826d2	08846c7a-ca77-49bf-a889-20aac620d62d	\N	150000.00	PENDING	\N	ООО СОМ	1234567890	\N	123123123	12312312312312312311		ПАО Сбербанк	Оплата 	РЖД-1772373007-276	\N	2026-03-01 16:50:07.538518+03	\N	\N	\N
b1da9081-1d67-4283-b8cf-a7df1b8b13ba	3fb5bf94-325e-4faf-b8ee-d6b86dc2ecd8	pay_dfda6844cc154afa	150000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	Оплата	РЖД-1772376816-293	2026-03-01 17:53:36.727655+03	2026-03-01 17:53:36.72856+03	2026-03-01 17:53:36.727668+03	\N	Перевод со счета 40702810123451230298 (баланс был: 500000,00, стал: 500000,00) на счет РЖД 40702810900000000001 (баланс был: 10300002,00, стал: 10300002,00). Сумма: 150000,00
20091316-0f4f-4dc1-9637-15a6cf87f58e	30bc7f04-cbb7-4759-9293-2d151dd1f89f	pay_589031f6cbeb4ced	150000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	Оплата	РЖД-1772377844-047	2026-03-01 18:10:44.240407+03	2026-03-01 18:10:44.242645+03	2026-03-01 18:10:44.240423+03	\N	Перевод со счета 40702810123451230298 (баланс был: 350000,00, стал: 350000,00) на счет РЖД 40702810900000000001 (баланс был: 10450002,00, стал: 10450002,00). Сумма: 150000,00
1eeef363-e0ae-40e6-83f8-9ef76bd6c891	9002a813-e25b-4b9b-bc13-34f5d7db94cd	pay_93610ed0cf0b42e7	150000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	оплата	РЖД-1772378269-239	2026-03-01 18:17:49.936561+03	2026-03-01 18:17:49.939128+03	2026-03-01 18:17:49.936575+03	\N	Перевод со счета 40702810123451230298 (баланс был: 50000000,00, стал: 50000000,00) на счет РЖД 40702810900000000001 (баланс был: 10600002,00, стал: 10600002,00). Сумма: 150000,00
251384fc-2e00-43d3-81fe-f303f8cb61ca	ba29549e-83a1-4645-bba9-f6afb20572f9	pay_af501a9d5b824893	191000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	ыва	РЖД-1772380224-862	2026-03-01 18:50:24.778879+03	2026-03-01 18:50:24.780875+03	2026-03-01 18:50:24.778891+03	\N	Перевод со счета 40702810123451230298 (баланс был: 49850000,00, стал: 49850000,00) на счет РЖД 40702810900000000001 (баланс был: 10750002,00, стал: 10750002,00). Сумма: 191000,00
eba17e77-9ce8-4033-9f9c-5b5f7dd35341	24938503-1b08-41a7-a54b-9a4de5f7b21c	pay_64a2726d37e642c3	191000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	ыва	РЖД-1772380800-748	2026-03-01 19:00:00.140314+03	2026-03-01 19:00:00.141647+03	2026-03-01 19:00:00.140331+03	\N	Перевод со счета 40702810123451230298 (баланс был: 49659000,00, стал: 49659000,00) на счет РЖД 40702810900000000001 (баланс был: 10941002,00, стал: 10941002,00). Сумма: 191000,00
52c02df6-ac6e-4a33-97b6-db61b7c6002d	a626deca-2889-4f92-bccb-91f03db4b009	pay_ab94348ceca54e72	191000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	ыв	РЖД-1772381026-757	2026-03-01 19:03:46.316169+03	2026-03-01 19:03:46.317196+03	2026-03-01 19:03:46.316188+03	\N	Перевод со счета 40702810123451230298 (баланс был: 49468000,00, стал: 49468000,00) на счет РЖД 40702810900000000001 (баланс был: 11132002,00, стал: 11132002,00). Сумма: 191000,00
48d595d7-da93-4f3e-a503-cd276f5c168a	04b16866-24ce-486d-a463-f02df4e12fc4	pay_1334a771bd8b4c91	191000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	ыва	РЖД-1772381280-036	2026-03-01 19:08:00.497255+03	2026-03-01 19:08:00.498554+03	2026-03-01 19:08:00.497267+03	\N	Перевод со счета 40702810123451230298 (баланс был: 49277000,00, стал: 49086000,00) на счет РЖД 40702810900000000001 (баланс был: 11323002,00, стал: 11514002,00). Сумма: 191000,00
5fef6404-b461-4881-af48-def6a340dfae	5b327642-63b4-4250-99b0-d3edce86f7d0	pay_53e392c4057f4afd	241000.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	sdf	РЖД-1772461626-905	2026-03-02 17:27:06.108074+03	2026-03-02 17:27:06.110126+03	2026-03-02 17:27:06.108089+03	\N	Перевод со счета 40702810123451230298 (баланс был: 49086000,00, стал: 48845000,00) на счет РЖД 40702810900000000001 (баланс был: 11514002,00, стал: 11755002,00). Сумма: 241000,00
8c176dea-e0bb-4fce-b0e0-fa22f49a5f56	d3f7eb05-e525-4dad-8850-41d691c78313	pay_1bb8db52dc514167	202860.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	два	РЖД-1772462477-161	2026-03-02 17:41:17.619387+03	2026-03-02 17:41:17.621952+03	2026-03-02 17:41:17.619396+03	\N	Перевод со счета 40702810123451230298 (баланс был: 48845000,00, стал: 48642140,00) на счет РЖД 40702810900000000001 (баланс был: 11755002,00, стал: 11957862,00). Сумма: 202860,00
204249a5-54cf-4cd8-83f6-9b10925020a5	038e92ca-da8a-4b71-ac4d-db596cfd40e8	pay_fdf964e008d94a8c	242860.00	SUCCEEDED	\N	ООО Абас	1234512345	\N	044525225	40702810123451230298		ПАО СБЕРБАНК	фыв	РЖД-1772463697-882	2026-03-02 18:01:37.883365+03	2026-03-02 18:01:37.883885+03	2026-03-02 18:01:37.883373+03	\N	Перевод со счета 40702810123451230298 (баланс был: 48642140,00, стал: 48399280,00) на счет РЖД 40702810900000000001 (баланс был: 11957862,00, стал: 12200722,00). Сумма: 242860,00
\.


--
-- Data for Name: station_distances; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.station_distances (id, from_station, to_station, distance_km, updated_at) FROM stdin;
a3fb16e1-5162-45a6-aaf3-e5a03c4de92b	Москва-Товарная	Санкт-Петербург-Товарный	650	2026-02-27 19:58:17.459085+03
93433509-1f1a-4b84-af82-e953e8665d67	Москва-Товарная	Екатеринбург-Товарный	1667	2026-02-27 19:58:17.459085+03
9d53202e-0e59-4498-85c3-5c75bf9f61b6	Москва-Товарная	Новосибирск-Товарный	2811	2026-02-27 19:58:17.459085+03
fc559481-7339-4281-9061-e702ef645617	Санкт-Петербург-Товарный	Москва-Товарная	650	2026-02-27 19:58:17.459085+03
6cd7f5fd-4375-415d-b3a6-c4c764465e3f	Москва-Товарная	Казань-Товарная	797	2026-03-01 20:57:23.739106+03
b60534a2-1e1e-4f2a-b5df-3394d0f5f997	Москва-Товарная	Нижний Новгород-Товарный	442	2026-03-01 20:57:23.739106+03
869c96d2-38de-47bb-8b6e-6e75c8431f10	Москва-Товарная	Самара	1049	2026-03-01 20:57:23.739106+03
31ed3b09-ce0d-4e1c-8798-9d3b0187ec6d	Москва-Товарная	Челябинск	1919	2026-03-01 20:57:23.739106+03
579775a2-562e-4c2b-b634-232968409a79	Москва-Товарная	Омск	2556	2026-03-01 20:57:23.739106+03
6947b0ac-47c2-4a71-809a-e713181987d9	Москва-Товарная	Красноярск	3955	2026-03-01 20:57:23.739106+03
57161a93-472b-432b-8b30-024aba869732	Москва-Товарная	Владивосток	9259	2026-03-01 20:57:23.739106+03
985a00b7-262b-4ad2-b145-7b2a0b2fcb76	Санкт-Петербург-Товарный	Мурманск	1240	2026-03-01 20:57:23.739106+03
2bd4f3a3-af90-4485-8cee-ee1cd2e96d51	Санкт-Петербург-Товарный	Архангельск	1050	2026-03-01 20:57:23.739106+03
882469cb-b1bf-40be-83a4-097ffec9acdf	Екатеринбург-Товарный	Тюмень	328	2026-03-01 20:57:23.739106+03
3bda676f-fbcb-4ee9-be38-a175e11a62ce	Екатеринбург-Товарный	Челябинск	220	2026-03-01 20:57:23.739106+03
91501721-9642-4594-a5c5-136b133d3e02	Новосибирск-Товарный	Красноярск	758	2026-03-01 20:57:23.739106+03
3b0432e0-850c-4203-87ea-2870e1d543a4	Новосибирск-Товарный	Омск	627	2026-03-01 20:57:23.739106+03
70043e1e-601e-4eb9-8264-77256c7307a4	Казань-Товарная	Нижний Новгород-Товарный	547	2026-03-01 20:57:23.739106+03
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariffs (id, cargo_type, wagon_type, base_rate, coefficient, description, created_at, updated_at) FROM stdin;
a4a33e7e-d535-4e2d-a318-031d96e75257	Электроника	крытый	15.50	1.20	Тариф для электроники в крытых вагонах	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
63c9bd04-3d8a-4d16-aee2-0a894570ef3c	Электроника	полувагон	14.00	1.10	Тариф для электроники в полувагонах	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
3a27ff8c-dbeb-494c-8d06-822278f533dd	Уголь	полувагон	8.50	1.00	Тариф для угля	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
d0843e09-4fe9-419c-8e45-ea978ae36c80	Нефть	цистерна	12.00	1.30	Тариф для нефтепродуктов	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
fde2e1f2-02df-45a7-862d-0ef0b364a262	Металл	платформа	11.50	1.10	Тариф для металлопроката	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
3dee34b7-315e-4be0-b8e0-adf2ab8f09d0	Лес	полувагон	9.00	1.00	Тариф для леса	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
1c23dae4-83a8-41f4-8e26-b4726c5d5b03	Оборудование	крытый	18.00	1.40	Тариф для оборудования	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
6213d18d-af22-471b-b390-b1e0e893c6b0	Зерно	крытый	7.50	0.90	Тариф для зерна	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
4233954e-a664-44e9-afd8-85ed6e6d6042	Химия	цистерна	13.50	1.50	Тариф для химических грузов	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
b97de8ce-98e0-42b7-a0e6-a399a8313c94	Контейнеры	платформа	16.00	1.20	Тариф для контейнеров	2026-02-26 22:57:18.983889+03	2026-02-26 22:57:18.983889+03
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, company_name, inn, created_at, role) FROM stdin;
a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11	logistics@vector.ru	hashed_pwd_1	ООО Вектор	7701234567	2026-02-26 19:44:19.238275+03	USER
b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22	supply@romashka.ru	hashed_pwd_2	ЗАО Ромашка	7709876543	2026-02-26 19:44:19.238275+03	USER
6705048b-12f0-4caa-8a44-2a8040a5fc58	test100@logistic.ru	$2a$10$TSCnBvxReIylC.bkOMh05.IV6HYyCl1K22UEFxkepIVtzFr.Crjku	ООО Тест	7712345678	2026-02-27 11:27:42.961501+03	USER
bfb12d5f-0f66-4461-a4ef-6f7a1cf31418	test101@logistic.ru	$2a$10$QsE7rM32t4dg.JvHXlk82eSNi8VZhM3MMqNYdUNaRoOaAOvuUEOu2	ООО Тест	7712345678	2026-02-27 12:33:51.849151+03	USER
0f22dba8-eb37-460f-acd2-f1fe8ec324e3	som@mail.ru	$2a$10$CQEIsz429se9IaUWB2vJfeOc4R/YJcjp.yeBJytqsOKRK85fsdehO	ООО СОМ	1234567890	2026-02-27 18:26:57.811742+03	USER
7ccda00f-6f5b-49cf-9d92-36a4d81a3f7a	abas@mail.ru	$2a$10$CngcSQivcSU4GGz9i4UeOeb0DsbFnG5lh25Bxo.bWTbr2acmrNnN.	ООО Абас	1234512345	2026-03-01 17:47:30.955325+03	USER
\.


--
-- Data for Name: wagon_schedule; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagon_schedule (id, wagon_id, order_id, departure_station, arrival_station, departure_date, arrival_date, status, cargo_type, cargo_weight_kg, created_at) FROM stdin;
9f13f9c6-4e42-4a38-ae8c-61da28944075	bad79eb0-3c4f-45cf-be5e-22ef2d710159	fddc22f3-131f-4c38-9b2f-4c575da6f68b	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 17:07:28.252995+03
3e000635-1f88-4aec-a675-9869a698bd5e	33333333-3333-3333-3333-333333333333	c03ea3a2-d321-45c8-be65-92ae0733b122	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 17:36:30.564357+03
3e4af49b-8dc8-49f0-b174-2bce3b326276	9ea78294-4040-47be-a089-d695dac0293a	bfbc2a9d-f509-45f1-8904-b44bbef41ebf	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 18:40:57.310725+03
9a9592d3-542c-45d0-a3f7-f3fbbca91397	3a0c9f8e-589a-4749-af2c-40a0b61d56a4	745436ad-2d6a-4876-a6de-8568efe541b0	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 18:57:13.470212+03
b1db5d0e-38c0-44ba-87d1-b33490452b56	22222222-2222-2222-2222-222222222222	326a5002-b4ef-4f91-ba1c-4910fdd4c31a	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 19:00:19.40564+03
5ec4839a-0f40-419f-b8dc-deada5ddd8cd	20e6caa1-3f1b-4349-a31c-6c94c257a385	3d507002-c7f6-4f3a-a95a-5d05c671378f	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 19:39:48.751473+03
df449055-2409-405b-9575-d4b946d4d5c3	8b6a7487-0555-49c4-a872-6cf7818f1bd0	cca64592-39ed-4e53-a84f-e003e4510a65	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 19:52:40.856203+03
060943df-47b4-4da5-8db1-bd6eedf2873d	cccccccc-cccc-cccc-cccc-cccccccccccc	01a973bc-6112-4328-9b34-5e9c7e7d1791	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 20:36:12.948386+03
7b4071db-b551-427f-9ad9-6d4839c36140	eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee	ddd2c250-ada7-4161-8416-8969e13d7b22	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 20:47:58.884338+03
52fa31a5-13e0-4540-9458-7be2c05389d1	dddddddd-dddd-dddd-dddd-dddddddddddd	b0f90637-acf7-4a17-a88b-cbfe85e3e73c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 20:51:41.883227+03
066d21f4-673f-4eb9-8a0e-391f3dc8332b	33333333-4444-5555-6666-777777777777	075ed92c-a7d4-4539-9637-df56638e06d6	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 15:49:30.874367+03
f09916b7-799e-415b-9f9b-e40cff68e866	66666666-7777-8888-9999-000000000000	4fd91b6e-ed4f-4df0-989b-5fd9dc506e66	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 16:28:50.98421+03
fade53c0-9b2b-4718-8971-cff7478030ff	ffffffff-ffff-ffff-ffff-ffffffffffff	08846c7a-ca77-49bf-a889-20aac620d62d	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 16:49:45.887344+03
d01e29b6-dc39-4cac-b7bd-c91399143107	44444444-5555-6666-7777-888888888888	3fb5bf94-325e-4faf-b8ee-d6b86dc2ecd8	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 17:48:22.756226+03
6b4e24e4-3652-4fb1-8b9c-dbc2dde5ff1c	55555555-6666-7777-8888-999999999999	864ad7f7-0c52-4870-b231-b7d88b439df7	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:03:36.288954+03
6edbd19e-478a-49eb-b263-e7bafa3d4095	aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa	30bc7f04-cbb7-4759-9293-2d151dd1f89f	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:08:20.162761+03
843ed351-335c-407c-9c33-bda334dc14d8	bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb	9002a813-e25b-4b9b-bc13-34f5d7db94cd	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:17:14.134703+03
1e6032e1-8578-4153-a646-cd1ae85d122a	11111111-2222-3333-4444-555555555555	a4c32e9f-cc37-45af-a0ed-d5ce900e575c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:20:52.802195+03
ef3415c2-02b5-4d48-b058-48c603831a91	22222222-2222-2222-2222-333333333333	1c17dce3-8bce-48f4-a0e1-5a4ce02d5fb6	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:27:49.519834+03
21373299-39c2-4c4d-bd11-b378edc37950	11111111-1111-1111-1111-dddddddddddd	122d8026-a0be-4848-8960-c3a8d413cbff	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:30:16.582367+03
37ccf4c5-8679-4120-93d6-4bcb6f83acce	b6d26987-ecfd-4d17-a46c-693d199b299a	8e6f3fe3-4f1f-4c69-86da-8ebc1b1ad13c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:36:50.11649+03
99b71e4b-e894-4619-aaf6-fd7bfafc3f5e	22222222-2222-2222-2222-123123123121	41d598e2-8442-4d0a-af06-568498479b0d	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:42:41.707935+03
f116fd77-a4fa-45c4-825e-9dd4009cb7bd	11111111-1111-1111-1111-756757576756	ba29549e-83a1-4645-bba9-f6afb20572f9	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:49:57.792027+03
b59a5459-2972-40b6-8bcf-6ad928c66c53	22222222-2222-2222-2222-789789644535	24938503-1b08-41a7-a54b-9a4de5f7b21c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:59:21.300707+03
601f4a4d-9212-4dec-9ec2-4d33905cf24a	22222222-2222-2222-2222-213124354656	a626deca-2889-4f92-bccb-91f03db4b009	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 19:03:22.694388+03
0d1a17e2-8034-49d8-a2bf-32b93021a4d9	11111111-1111-1111-1111-345453522322	04b16866-24ce-486d-a463-f02df4e12fc4	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 19:07:37.415815+03
13f70041-3862-4049-869c-5bd2b4d80c73	bad79eb0-3c4f-45cf-be5e-22ef2d710159	118e84d3-3537-4b08-a09f-75ce9135641f	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-02 17:15:36.691891+03
f0daad3d-0d9c-417f-be96-83ea9974addb	22222222-2222-2222-2222-333333333333	fcf6581c-32bd-4290-b693-f8ca770059c1	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-02 17:20:18.382002+03
5957971e-e3ba-4996-9961-900cb1a95f78	11111111-1111-1111-1111-111111111111	e2522210-3b56-4289-bdd5-04fc205908b2	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-02-28 14:22:25.645995+03
7fadfd56-5c96-48a3-b99c-57e3b4846c3d	11111111-1111-1111-1111-454545454545	6e62691c-ac8d-49fe-b061-f54f61e7a08f	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-01 18:46:16.344472+03
af35351d-fa14-44a8-92d3-b52b35425ce1	11111111-1111-1111-1111-454545454545	d3f7eb05-e525-4dad-8850-41d691c78313	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 17:40:53.123506+03
2b855e58-c99b-4123-a426-8bc9bcc776e6	11111111-1111-1111-1111-111111111111	5b327642-63b4-4250-99b0-d3edce86f7d0	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 17:26:21.532041+03
999f67b4-bd0b-45ae-9cca-90ce3de876ec	11111111-1111-1111-1111-111111111111	2931a42e-ed65-44da-be5c-1d73a4a7005c	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 17:47:52.719891+03
f8ec8524-eca1-441b-bedf-261a6d10afe1	11111111-1111-1111-1111-111111111111	9cfdbf9b-4fee-41b6-804d-d717059207ae	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 17:49:11.321911+03
3d734009-e13b-4fdc-883e-ac61143b3a26	11111111-1111-1111-1111-111111111111	f769ca26-2248-4a81-b5e6-ae955e8054f7	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 17:54:26.466228+03
3a80e113-068a-4c93-9727-a5971035e7bd	11111111-1111-1111-1111-111111111111	038e92ca-da8a-4b71-ac4d-db596cfd40e8	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-02 18:01:22.578601+03
\.


--
-- Data for Name: wagon_tariffs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagon_tariffs (id, wagon_type, cargo_type, base_rate_per_km, coefficient, min_price, description) FROM stdin;
946b0a24-e152-4771-8db4-a3cf46835386	крытый	Электроника	15.50	1.20	5000.00	\N
bc54feb1-3779-4d02-a692-d70d899c333c	крытый	Оборудование	18.00	1.40	6000.00	\N
f708b4a1-aa2b-48fd-a460-cfce465f2a7b	крытый	Металл	12.00	1.10	4500.00	\N
bfdef634-c4e2-4b96-9f55-9d8336cd1dfc	крытый	Уголь	10.00	1.00	3500.00	\N
ed361805-f32c-4c5f-96b2-979fb6a0e628	крытый	Нефть	13.00	1.20	5000.00	\N
843b748d-6358-4052-b3ab-99daac998486	крытый	Лес	11.00	1.00	4000.00	\N
cd4e89be-fd49-4ea1-8ffe-355972d8ce11	крытый	Зерно	9.50	0.90	3500.00	\N
42e3feb0-540b-4999-9ed3-989eb4edc1ff	крытый	Химия	14.00	1.30	5500.00	\N
b6c11a79-c110-4f91-a603-18803ee29e65	крытый	Контейнеры	12.50	1.00	4500.00	\N
d3dcfa9f-77ef-4ba4-b293-43ced342d8ae	крытый	Трубы_стальные	12.50	1.10	4500.00	\N
3dcee42f-ab0a-4891-9f9f-88f9ac791308	крытый	Стандартный	14.00	1.00	4000.00	\N
f94dac05-f62f-4700-9bff-632db3cdaf27	крытый	Сыпучий	10.50	1.00	3500.00	\N
6e35d4da-f13c-46cb-ae6a-b7989df867fb	крытый	Наливной	13.50	1.20	5000.00	\N
0ee87e90-f8a2-4dd5-a400-fb8680ee6a4f	крытый	Опасный	16.00	1.50	7000.00	\N
6530e108-c12d-40c4-8a80-86ba7ea0e1cc	крытый	Хрупкий	17.00	1.30	6000.00	\N
b9a8ac11-f585-41ba-b744-cc57ec9ea98d	крытый	Негабаритный	19.00	1.60	8000.00	\N
9dd5a1ab-9da6-44f9-b7c1-97c1c845fc9b	крытый	общий	14.00	1.00	4000.00	\N
f6e3a763-fe6b-4a72-9e49-cd6093e37820	полувагон	Уголь	8.50	1.00	3500.00	\N
41c8e4b0-c0db-49a0-8e65-1b0b67e4ab8d	полувагон	Металл	9.50	1.10	4000.00	\N
39434111-ca5f-4998-83b0-82b307d92b36	полувагон	Лес	9.00	1.00	3800.00	\N
775b5acf-4f62-4649-b443-eeeb78f86a07	полувагон	Руда	8.80	1.10	3500.00	\N
73683ad9-52ec-477c-af1c-885ec796b270	полувагон	Щебень	7.50	0.90	3000.00	\N
06bb1291-6af8-402e-b77c-aa870b70fe0e	полувагон	Зерно	8.00	0.90	3200.00	\N
159cd97e-af4f-467e-aa9b-927fd7dd150f	полувагон	Оборудование	10.50	1.20	4500.00	\N
7b2c3d93-b3ad-4d73-a21f-09e7c07dd076	полувагон	Электроника	11.00	1.20	4500.00	\N
22118ac9-1627-4d41-85fa-4901ad88fc7a	полувагон	Нефть	9.50	1.10	4000.00	\N
eae452b8-aa73-46b1-b97d-358ef198975f	полувагон	Химия	10.00	1.20	4200.00	\N
aff97b80-30c0-4f21-88ab-425829439b11	полувагон	Контейнеры	9.80	1.00	3800.00	\N
b397128b-c390-412b-9ec1-9305cd3d732a	полувагон	Трубы_стальные	9.20	1.10	4000.00	\N
4d2ee51b-8a18-47ad-b1af-d8b83011d158	полувагон	Стандартный	9.00	1.00	3500.00	\N
62a07967-366e-4cf4-abba-2d69cb9931cc	полувагон	Сыпучий	8.00	1.00	3000.00	\N
4003500a-884f-4b5e-a269-61d37b547d51	полувагон	Наливной	9.50	1.10	4000.00	\N
4410bd7f-54f3-4416-98f8-eb2d7b1e2340	полувагон	Опасный	11.00	1.40	5000.00	\N
207783af-8519-4dbc-98bc-a72904a912e6	полувагон	Хрупкий	12.00	1.30	4500.00	\N
dc280667-76c1-4f11-985e-662333fd437f	полувагон	Негабаритный	13.00	1.50	5500.00	\N
5b1ab03f-1b6d-44f9-a7b6-50739db210cc	полувагон	общий	8.50	1.00	3500.00	\N
0b5cf3e3-86e7-4639-ae8d-32ae78325a52	платформа	Контейнеры	11.00	1.00	4500.00	\N
625d3b8f-3ac0-439c-81d9-d017d5261f2a	платформа	Металл	11.50	1.10	5000.00	\N
bb5dad95-f1e4-49ff-b93b-2a6bf7e212e5	платформа	Оборудование	14.00	1.30	6000.00	\N
370e2e7d-04f1-4f0b-ab35-7347255f5ee5	платформа	Лес	10.50	1.00	4000.00	\N
9abea5a3-991c-47c9-9fb7-a10a490a82b2	платформа	Трубы_стальные	11.00	1.10	4500.00	\N
c0216398-3654-4e02-a312-f073812183dd	платформа	Уголь	9.00	1.00	3500.00	\N
9df96914-f589-40cd-81a4-70b917dcc3ea	платформа	Нефть	10.00	1.10	4000.00	\N
d9773e69-50f8-4970-b951-51a730113fe0	платформа	Химия	11.50	1.20	4800.00	\N
340acff2-45f7-4451-95c2-c7692e273073	платформа	Электроника	12.00	1.20	5000.00	\N
f06bc8bf-32dc-4ebc-bdd4-74ad1884a28a	платформа	Зерно	8.50	0.90	3200.00	\N
3ab4b693-d35f-442a-a716-4c5494700615	платформа	Стандартный	10.00	1.00	3500.00	\N
f9a371f6-83ca-4787-a1f3-f9049840265b	платформа	Сыпучий	9.00	1.00	3300.00	\N
1e873c65-df45-4d9f-ba1b-8740f8323a10	платформа	Наливной	10.50	1.10	4000.00	\N
396bd585-a483-476c-b3b0-748a45029717	платформа	Опасный	13.00	1.40	5500.00	\N
4b878851-f8b0-4939-87f0-e8e59610f5e1	платформа	Хрупкий	14.00	1.30	5000.00	\N
5d3dfc77-ddda-4474-99e0-5a2ef00dc39f	платформа	Негабаритный	15.00	1.50	6500.00	\N
39e0dfb8-1363-4fe1-bc65-12162010db46	платформа	общий	10.00	1.00	3500.00	\N
9e8730b4-0660-4843-9378-b54aeb73535b	цистерна	Нефть	14.00	1.30	6000.00	\N
83ef3ece-891f-4f1e-b97c-2be920b4dc0a	цистерна	Химия	13.50	1.50	6500.00	\N
fa10b340-43bb-4f75-8fe7-542fa2fd4e71	цистерна	Металл	12.00	1.20	5500.00	\N
8c816b50-bbb8-4065-ad97-6f39da439066	цистерна	Бензин	14.50	1.40	6000.00	\N
639640e5-8bac-4151-af0b-6780bef6ae53	цистерна	Газ	16.00	1.60	7000.00	\N
a14fde51-76e9-460e-80a5-a1d67dc79d2d	цистерна	Молоко	13.00	1.20	5500.00	\N
25ad4e9d-f316-4db1-9676-d631848275a8	цистерна	Спирт	15.00	1.50	6500.00	\N
cd7e27c2-1fee-472f-b671-08a9b76375d2	цистерна	Кислота	14.50	1.60	7000.00	\N
a42b49ad-b6b1-4df3-b057-74c476e9cc25	цистерна	Вода	11.00	1.00	4500.00	\N
7bdb7a01-5164-4796-a6c3-2529f9aeeb73	цистерна	Пиво	13.00	1.20	5500.00	\N
f50002ea-68f4-4dad-9db5-61bfaf6869a3	цистерна	Оборудование	12.50	1.20	5000.00	\N
1d09d781-0026-4dd9-9c00-2dcb266dd2a5	цистерна	Электроника	13.00	1.20	5500.00	\N
839fcd2d-de00-4411-8ea9-cd18a9e0dcfb	цистерна	Уголь	10.00	1.00	4000.00	\N
cdb072d3-4a65-4ced-96e1-373a5681db32	цистерна	Лес	9.50	1.00	3800.00	\N
0854b272-e20c-4ba1-bc33-8bb6cf8150a3	цистерна	Зерно	9.00	0.90	3500.00	\N
635378a7-acc2-49bf-b3c6-c647c9c3a04f	цистерна	Контейнеры	11.00	1.00	4500.00	\N
10cae2cd-ef25-46db-b6e9-1529e9e396b8	цистерна	Трубы_стальные	11.50	1.10	5000.00	\N
ee4ce745-e2ed-438f-909c-b9bdc4af5ee2	цистерна	Стандартный	12.00	1.00	5000.00	\N
fc680935-2673-425c-8bbc-f3de54bf63a3	цистерна	Сыпучий	10.00	1.00	4000.00	\N
427bbea1-b2fd-4c41-9db1-9558256af1b2	цистерна	Наливной	14.00	1.30	6000.00	\N
f0b05510-25c5-4254-981a-2046a117c35b	цистерна	Опасный	15.00	1.50	7000.00	\N
72dc2427-56be-4828-b587-deb9bbcb327c	цистерна	Хрупкий	16.00	1.40	6500.00	\N
26e9097c-0c3f-4732-939d-0a35a9b81734	цистерна	Негабаритный	17.00	1.60	7500.00	\N
5f9ac8b0-b0c0-4360-8288-fe9cc8ce4a7d	цистерна	общий	12.00	1.00	5000.00	\N
f404d2d8-9978-4639-b985-0b59aa9f2836	рефрижератор	Продукты	22.00	1.40	8000.00	\N
5ddb4344-031b-4120-a038-903e4ea2e0a2	рефрижератор	Медикаменты	25.00	1.60	10000.00	\N
f05cbf8b-4ebf-4061-9df4-559072c9cf28	рефрижератор	Цветы	24.00	1.50	9000.00	\N
63cc8bc5-7a58-4f65-a738-04bccf9a4896	рефрижератор	Мясо	23.00	1.40	8500.00	\N
04f4d213-16bb-49a4-9fe8-5834a260b09e	рефрижератор	Рыба	23.00	1.40	8500.00	\N
9ab40574-d2cd-431e-a62d-4a8601b0b7c7	рефрижератор	Овощи	21.00	1.30	7500.00	\N
b8b497df-ca57-4a22-9970-f342b0c27bd2	рефрижератор	Фрукты	22.00	1.30	7800.00	\N
c9b8b59f-4a74-4a5f-b92e-a5948fd8f4a5	рефрижератор	Мороженое	24.00	1.50	9000.00	\N
92e81ca2-02b7-40be-8aab-6b52db078bac	рефрижератор	Вакцины	26.00	1.70	11000.00	\N
ef81026f-f64d-4b3f-b88d-bdab82107fc5	рефрижератор	Химия	20.00	1.40	8000.00	\N
0c84a008-731d-43c3-9aa2-6277ec0d8d96	рефрижератор	Оборудование	19.00	1.30	7000.00	\N
4aeaf1bd-b93d-4627-af03-16c241d7f74c	рефрижератор	Электроника	20.00	1.30	7500.00	\N
5dfe3e91-88d1-454a-994b-9d494646d7a2	рефрижератор	Металл	18.00	1.20	6500.00	\N
e530bf80-8219-4b30-9599-d194c3bf126b	рефрижератор	Уголь	15.00	1.00	5500.00	\N
1f2d132a-9210-48db-94eb-0a10d70a3c26	рефрижератор	Нефть	17.00	1.20	6000.00	\N
1ad158d2-7b4e-4c1e-b025-2c44675284c9	рефрижератор	Лес	16.00	1.00	5500.00	\N
0706db47-00d3-45a5-9e57-3efc28434a95	рефрижератор	Зерно	15.00	1.00	5000.00	\N
4f8c7c58-cde0-4d0d-b87d-6a2dff8fce9d	рефрижератор	Контейнеры	18.00	1.10	6500.00	\N
5616ebfe-47ea-4d9d-91c1-7e4052aba212	рефрижератор	Трубы_стальные	17.00	1.10	6000.00	\N
c0de53dd-6d2d-486f-8e08-3f9289d835ea	рефрижератор	Стандартный	20.00	1.00	7000.00	\N
75c24eaa-d21b-4151-9ec4-abc75dc797e1	рефрижератор	Сыпучий	16.00	1.00	5500.00	\N
c538a145-441d-49f4-967e-9a1ec6f73b66	рефрижератор	Наливной	18.00	1.20	6500.00	\N
ac127b68-ea6c-48f2-936f-abd666dcd5da	рефрижератор	Опасный	22.00	1.50	9000.00	\N
dc1b9e45-142f-4b9c-a8be-51ad1afa1649	рефрижератор	Хрупкий	23.00	1.40	8500.00	\N
d854d52b-fd80-4fef-b90d-f3bde118f642	рефрижератор	Негабаритный	24.00	1.60	9500.00	\N
e965eb62-189a-4c65-88cc-4fb032d495c4	рефрижератор	общий	20.00	1.00	7000.00	\N
\.


--
-- Data for Name: wagons; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagons (id, wagon_number, wagon_type, max_weight_kg, max_volume_m3, current_station, status) FROM stdin;
bad79eb0-3c4f-45cf-be5e-22ef2d710159	99999999	крытый	70000	140	Москва-Товарная	забронирован
22222222-2222-2222-2222-333333333333	54353554	крытый	71000	120	Уфа	забронирован
11111111-1111-1111-1111-454545454545	45456445	крытый	68000	120	Москва	свободен
11111111-1111-1111-1111-111111111111	12345678	крытый	68000	120	Москва-Товарная	забронирован
44444444-4444-4444-4444-444444444444	55667788	платформа	72000	0	Санкт-Петербург	свободен
fb72c4d8-4f43-4275-a30f-8e5a25db970d	77777777	платформа	75000	0	Москва-Товарная	свободен
fc27171d-8046-4d10-b452-c95bad1354e8	22222222	платформа	72000	0	Новосибирск	свободен
33333333-3333-3333-3333-333333333333	11223344	крытый	68000	120	Казань	свободен
9ea78294-4040-47be-a089-d695dac0293a	33333333	полувагон	71000	85	Казань	свободен
3a0c9f8e-589a-4749-af2c-40a0b61d56a4	44444444	крытый	68000	120	Санкт-Петербург	свободен
22222222-2222-2222-2222-222222222222	87654321	полувагон	71000	85	Москва-Товарная	свободен
20e6caa1-3f1b-4349-a31c-6c94c257a385	88888888	полувагон	72000	90	Москва-Товарная	свободен
8b6a7487-0555-49c4-a872-6cf7818f1bd0	55555555	рефрижератор	65000	110	Москва-Товарная	свободен
22222222-3333-4444-5555-666666666666	80808080	крытый	69500	150	Нижний Новгород	свободен
cccccccc-cccc-cccc-cccc-cccccccccccc	30303030	крытый	68500	155	Красноярск	свободен
eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee	50505050	полувагон	71500	145	Челябинск	свободен
dddddddd-dddd-dddd-dddd-dddddddddddd	40404040	рефрижератор	66000	130	Владивосток	свободен
33333333-4444-5555-6666-777777777777	90909090	полувагон	72500	140	Омск	свободен
66666666-7777-8888-9999-000000000000	14141414	крытый	70500	165	Воронеж	свободен
ffffffff-ffff-ffff-ffff-ffffffffffff	60606060	платформа	74000	160	Самара	свободен
44444444-5555-6666-7777-888888888888	12121212	рефрижератор	65500	125	Уфа	свободен
55555555-6666-7777-8888-999999999999	13131313	платформа	73500	170	Пермь	свободен
aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa	10101010	платформа	73000	140	Екатеринбург	свободен
bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb	20202020	цистерна	69000	110	Новосибирск	свободен
11111111-2222-3333-4444-555555555555	70707070	цистерна	67500	115	Ростов-на-Дону	свободен
11111111-1111-1111-1111-dddddddddddd	12312312	крытый	68000	120	Москва	свободен
b6d26987-ecfd-4d17-a46c-693d199b299a	66666666	цистерна	68000	80	Москва-Товарная	свободен
22222222-2222-2222-2222-123123123121	78678667	крытый	71000	120	Уфа	свободен
11111111-1111-1111-1111-756757576756	12312322	крытый	68000	120	Москва	свободен
22222222-2222-2222-2222-789789644535	45643633	крытый	71000	120	Уфа	свободен
22222222-2222-2222-2222-213124354656	23842347	крытый	71000	120	Уфа	свободен
11111111-1111-1111-1111-345453522322	34723472	крытый	68000	120	Москва	свободен
\.


--
-- Name: cargo cargo_order_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cargo
    ADD CONSTRAINT cargo_order_id_key UNIQUE (order_id);


--
-- Name: cargo cargo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cargo
    ADD CONSTRAINT cargo_pkey PRIMARY KEY (id);


--
-- Name: company_accounts company_accounts_account_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_accounts
    ADD CONSTRAINT company_accounts_account_number_key UNIQUE (account_number);


--
-- Name: company_accounts company_accounts_inn_account_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_accounts
    ADD CONSTRAINT company_accounts_inn_account_unique UNIQUE (inn, account_number);


--
-- Name: company_accounts company_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_accounts
    ADD CONSTRAINT company_accounts_pkey PRIMARY KEY (id);


--
-- Name: order_services order_services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_services
    ADD CONSTRAINT order_services_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_payment_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_payment_id_key UNIQUE (payment_id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: station_distances station_distances_from_station_to_station_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.station_distances
    ADD CONSTRAINT station_distances_from_station_to_station_key UNIQUE (from_station, to_station);


--
-- Name: station_distances station_distances_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.station_distances
    ADD CONSTRAINT station_distances_pkey PRIMARY KEY (id);


--
-- Name: tariffs tariffs_cargo_type_wagon_type_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_cargo_type_wagon_type_key UNIQUE (cargo_type, wagon_type);


--
-- Name: tariffs tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: wagon_schedule wagon_schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagon_schedule
    ADD CONSTRAINT wagon_schedule_pkey PRIMARY KEY (id);


--
-- Name: wagon_tariffs wagon_tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagon_tariffs
    ADD CONSTRAINT wagon_tariffs_pkey PRIMARY KEY (id);


--
-- Name: wagon_tariffs wagon_tariffs_wagon_type_cargo_type_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagon_tariffs
    ADD CONSTRAINT wagon_tariffs_wagon_type_cargo_type_key UNIQUE (wagon_type, cargo_type);


--
-- Name: wagons wagons_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagons
    ADD CONSTRAINT wagons_pkey PRIMARY KEY (id);


--
-- Name: wagons wagons_wagon_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagons
    ADD CONSTRAINT wagons_wagon_number_key UNIQUE (wagon_number);


--
-- Name: idx_company_accounts_inn; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_company_accounts_inn ON public.company_accounts USING btree (inn);


--
-- Name: idx_company_accounts_main; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_company_accounts_main ON public.company_accounts USING btree (inn, is_main) WHERE (is_main = true);


--
-- Name: idx_company_accounts_rzd; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_company_accounts_rzd ON public.company_accounts USING btree (is_rzd_account) WHERE (is_rzd_account = true);


--
-- Name: idx_payments_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_created_at ON public.payments USING btree (created_at);


--
-- Name: idx_payments_document; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_document ON public.payments USING btree (payment_document);


--
-- Name: idx_payments_inn; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_inn ON public.payments USING btree (inn);


--
-- Name: idx_payments_inn_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_inn_status ON public.payments USING btree (inn, status);


--
-- Name: idx_payments_order_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_order_id ON public.payments USING btree (order_id);


--
-- Name: idx_payments_payment_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_payment_id ON public.payments USING btree (payment_id);


--
-- Name: idx_payments_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payments_status ON public.payments USING btree (status);


--
-- Name: idx_schedule_dates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_schedule_dates ON public.wagon_schedule USING btree (departure_date, arrival_date);


--
-- Name: idx_schedule_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_schedule_status ON public.wagon_schedule USING btree (status);


--
-- Name: idx_schedule_wagon; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_schedule_wagon ON public.wagon_schedule USING btree (wagon_id);


--
-- Name: idx_station_distances_from_to; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_station_distances_from_to ON public.station_distances USING btree (from_station, to_station);


--
-- Name: idx_tariffs_cargo_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariffs_cargo_type ON public.tariffs USING btree (cargo_type);


--
-- Name: idx_tariffs_wagon_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_tariffs_wagon_type ON public.tariffs USING btree (wagon_type);


--
-- Name: users after_user_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER after_user_insert AFTER INSERT ON public.users FOR EACH ROW EXECUTE FUNCTION public.create_account_for_new_user();


--
-- Name: cargo cargo_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cargo
    ADD CONSTRAINT cargo_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: payments fk_payments_order; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: order_services order_services_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_services
    ADD CONSTRAINT order_services_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: orders orders_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: orders orders_wagon_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_wagon_id_fkey FOREIGN KEY (wagon_id) REFERENCES public.wagons(id) ON DELETE SET NULL;


--
-- Name: wagon_schedule wagon_schedule_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagon_schedule
    ADD CONSTRAINT wagon_schedule_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;


--
-- Name: wagon_schedule wagon_schedule_wagon_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wagon_schedule
    ADD CONSTRAINT wagon_schedule_wagon_id_fkey FOREIGN KEY (wagon_id) REFERENCES public.wagons(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

