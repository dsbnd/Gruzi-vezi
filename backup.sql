--
-- PostgreSQL database dump
--

\restrict 1qMxvUPzKo3N2OEKAc7KSVG6rsak4e9oHsLQLVB4CGgkHPyKoXxYkhtcB78WCdt

-- Dumped from database version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
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
4cafefd7-de6a-4eaa-a8ee-5b07af6fd1e7	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	Электроника	60000	100	Паллеты
2c281838-58b5-43d7-98ee-78cf97091b99	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	Трубы стальные	70000	40	Без упаковки
75c13d55-7790-4631-9167-857e098b0b70	58335374-87ab-4102-bb42-e7d7b82addca	STANDARD	25000	60	PALLET
6ac98345-9daa-482d-ba63-05daf47a3724	4880bfd6-ac26-4a77-b4d8-26427a9e5349	STANDARD	1	1	CONTAINER
6f9b0d76-4e77-4e5d-964c-1131419119ca	f27a903a-5004-411f-b719-309ae7dc1ca3	STANDARD	12	12	CONTAINER
10400de6-0d46-444e-a501-c3f129945d6c	a184a1a2-1b50-4d45-a208-e9eff8e2e81f	Стандартный	1	1	Контейнеры
28fdc679-bf5b-47c7-8dd6-3599c82e7478	0f6c65f2-61db-431a-a896-4667857fe8e6	Электроника	12	12	Паллеты
61d54afc-d395-4a51-9a96-15735c0c7bd3	f93a8445-1db6-4dcd-a51a-a0d56b9117a2	Электроника	12	12	Паллеты
ea9305e7-6e52-49d9-bf12-e182bcc18780	e8aa5fb0-b731-484a-91d0-05f4ee6366f8	Электроника	12	12	Паллеты
3f4e8105-fc69-4c94-bd1f-c2a026ca6d2b	bfcbe5a4-2268-4b7c-af15-6c7aec65394d	Электроника	12	12	Паллеты
4646621a-6df1-4016-8b97-c7953285a2cf	b58f7811-98ed-456c-9793-353dacb2ab0b	Электроника	12	12	Паллеты
9d67078a-0a28-4ffd-8843-fbd9eefccc14	a2e2f0b8-8368-4606-9bd6-b4ad0c3de6fe	Электроника	12	12	Паллеты
aa1e3b96-f355-40d8-878b-8908112909a4	f6a96f76-85d6-45a1-a149-1a70eba3c66b	Электроника	12	12	Паллеты
82ebad53-6fb7-42b7-a663-6827c8634916	2cfb03fe-376c-4a1a-b613-bbad1b6ef50d	Электроника	1	1	Паллеты
d7ffadeb-3c92-4695-a87d-45cd0bba45d5	2bf4500c-8b8d-491d-a348-4ad88a696ec2	Электроника	12	12	Паллеты
d9f12f57-d721-4b0e-b3ff-d8cf7a972f23	0cf26f67-8a08-4f69-873e-ca112939674f	Электроника	12	12	Паллеты
a11d3085-9ba5-4063-86f2-79856b35a945	f60a6ce4-5099-47b6-9fbd-3d62224a75e6	Электроника	12	12	Паллеты
e8dfe537-b77c-493d-9d33-3e328d912513	9abd9cdc-a3e1-476c-838e-5cbfa255886f	Электроника	12	12	Паллеты
c7124104-3123-4ad7-ac0e-6e8b2757419c	36c9524e-8a1b-428c-9409-69c4c89accdc	Электроника	123	12	Паллеты
ae3dff9d-ef6c-4826-b70d-ca6da4798a76	be084036-ac97-47d6-9b9a-7b635d8fb53c	Электроника	1	1	Паллеты
62d00c08-62b5-4a50-b97b-58643c79385c	5f9c941d-82cb-46a6-80df-6d689c3563a8	Электроника	1	1	Паллеты
8091d137-eb39-4cbf-b222-3234f1e7f264	340a7460-7b58-4d9f-b22e-f3f865b0fa78	Электроника	1	1	Паллеты
03ca5c3c-e497-45b6-b6c3-21fcbb1e5110	ee90a8ca-9900-45be-8eac-7b1472a1593c	Уголь	1	1	Паллеты
8ef5029c-c851-4c77-a833-92d9c542ab22	1f10acaf-fcf0-46ae-a28b-d3bc50364338	Уголь	1	1	Паллеты
b0ea83ce-ddfc-4197-bbc9-2efc91c6694d	92b3c5d7-3d3e-4063-a448-f7afd6946820	Уголь	1	1	Паллеты
6fa727db-d91e-40c7-9e3c-c1ddbb667849	a91edb6a-7177-455a-ad44-c82b1aee035e	Электроника	1	1	Паллеты
08a449bd-587f-43ff-9391-a804fc61d266	ee6d61bd-c741-40b8-82f8-e81312a1b37b	Уголь	1	1	Паллеты
e3e1d2c3-2de8-4189-9f39-f20c0a410682	93a28763-e8fc-42a2-97f9-d836d2e29063	Нефть	1	1	Паллеты
7817bf0e-5816-4452-9555-ee8d04f53909	fbd8a73d-4c5b-4fc2-873a-4d46a7675458	Металл	1	1	Паллеты
cc44ef21-0976-4989-ac6e-0f8b6076a924	1e9e9925-fec5-4b84-8d83-e99838bd95ce	Уголь	1	1	Паллеты
415613c7-51a9-4556-b757-4d38ccb2ba64	dcbd8e1c-8951-4ead-9cdb-a3419d487fe2	Электроника	12	12	Паллеты
a7f88a33-f63a-4c94-915a-f8126d22a2c8	c05591be-92c0-419d-b969-c6a1ff144cc8	Электроника	11	11	Паллеты
ecc479f8-3dad-4955-af1e-cf0e1dc728e1	4088f0d8-7f4b-484f-ae91-cb8fa4cb8559	Электроника	11	11	Паллеты
ff3e41b2-b691-4c41-8700-eadf78bda845	484131f3-7bcc-4e44-a3f1-2e88010889fe	Уголь	11	11	Паллеты
a56a9b5e-da0f-44e3-b12f-bddf285261f4	e8886610-6f86-40ac-81f8-67a8258e62ba	Электроника	12	12	Паллеты
a7ff9daf-0a16-4604-870f-44bff9126015	35c50ce5-acc4-44e6-b5e8-6bc7bb1fbb8d	Уголь	12	12	Паллеты
8aab3192-bf71-40d8-b2ba-b013bc410a99	15c5ce61-145e-47d3-9fdf-54b906da65af	Уголь	12	12	Паллеты
98dff992-7129-428b-975d-35b453c26c4c	46d29d04-69bc-4ed3-b976-7776f5ae3293	Нефть	12	12	Паллеты
e243c200-16d7-4fb8-8594-5b88ea36b2d2	c6fcfb05-496c-4c0e-b81c-e21e37eed19d	Нефть	12	12	Паллеты
c43414a4-d412-46a0-8aea-48e407e59c03	ecc023af-3c74-4d0d-97a9-ef6464026b48	Нефть	12	12	Паллеты
c9d0be7b-0db2-4031-adfd-e522ecd9c93c	23ccc339-e2d8-4789-8885-6aa68c6fce91	Электроника	12	12	Паллеты
344c9faa-f2d0-46db-bc66-615182136980	98ef3689-39a3-4885-8182-108de5857b97	Уголь	12	12	Паллеты
3cc1ce4e-f3f2-45bb-9b93-4d02e338136e	1e5dd479-62cf-4019-a4b4-5638f3d1471d	Нефть	12	12	Паллеты
ca2e0270-2669-4696-9c45-d7560074f62c	404e1650-a79a-4983-98b7-824cee346636	Нефть	12	12	Паллеты
802ea322-d465-4d28-bbdf-6dbaac05a06c	c14ee064-c29f-48fc-a2e8-65320aa32e27	Нефть	12	12	Паллеты
659fd98c-b71f-4767-8805-27ce18854659	55a38478-c0a0-4848-85c2-6fd5a1ea5e90	Уголь	12	12	Паллеты
9e7ecc75-4d3b-4b66-b933-53b46427c1ff	45cd68e6-c1a9-42b7-8bf0-c04ab1b0fd48	Уголь	12	12	Паллеты
91844e81-b917-4b96-a0cc-0cc018cf363c	a6e08faa-f073-403e-a6e6-e6b6c9eccb59	Металл	12	12	Паллеты
4ea28a08-1008-4143-86f4-ca9600518c69	3f2201d0-69d3-42a9-bf57-7370d0671907	Металл	12	12	Паллеты
71289b82-5873-42c7-aaeb-32f518766cc8	d7382b47-61a6-4ab5-9e8c-717d8029e71c	Металл	12	12	Паллеты
a3220eb5-0213-4ce5-b056-2a0357aab36b	1e1cc3c7-5e14-4590-ac2a-5dcc8e7dd90d	Лес	12	12	Паллеты
131cd259-aeac-44e3-a0f7-420f4da673de	aaceb290-ed3e-48af-ba7b-9df8ebb82365	Лес	12	12	Паллеты
87bc0423-4507-44d8-a57d-baa787396a03	fcb6ee71-a07b-4bf6-812b-577522ddaef2	Электроника	12	12	Паллеты
416d0b4c-5f29-42f0-97ab-56c2586120d7	ff5653ce-5024-45a6-a143-feeac30abd81	Электроника	12	12	Мешки
ea572984-73df-4ee6-9fcc-ef94fe3842b7	7e3060ef-1db0-49a6-aa44-344b0528df19	Электроника	12	12	Мешки
0271b1a7-8d86-4c2a-b456-64b170f9fb67	9f5ea48e-be24-4366-b4f7-d703eddc4b8e	Уголь	12	12	Мешки
255367bc-13eb-40c1-a638-e35201873bc5	ebc31a45-48eb-46ec-9fec-c7b65e443fb1	Нефть	12	12	Мешки
afe080a7-75f5-4c5e-8d64-da598709871e	b84a5336-b5b3-4b3f-8a33-607224a5b102	Металл	12	12	Мешки
3c943d7c-4b6c-4cf3-8674-166ac6e0be8d	5a5214b0-d87c-461e-b419-4acf8348100b	Лес	12	12	Мешки
8278d00b-b847-44ba-8ad9-c7a9f40361cf	8406ed16-9cd6-4e56-8f5f-140964c6f1a4	Оборудование	12	12	Мешки
06029a0c-a3c9-4cb1-96da-64a348a1f89f	28e76677-aac9-47a2-93da-611851970eaa	Зерно	12	12	Мешки
b3cfd6a8-44b7-425f-9584-f70d059ff69f	d4c5f1a4-a001-4c51-a360-8338af3c3cda	Зерно	12	12	Мешки
c9c6b9d0-cd64-465f-948b-6678cb29bca0	2f63ccb3-05a8-4ad6-8859-e472931433e4	Химия	12	12	Мешки
88f6ff0a-f94d-4c3d-9559-334ca3975b16	cbecae02-f6d0-40e1-8352-1ea33fc45630	Зерно	12	12	Мешки
1448e3f4-dc6b-434e-a797-d9c926ff6a6d	1eb2715b-c77c-4888-ae88-2710c71cd0d3	Зерно	12	12	Мешки
626ea957-c78f-4236-a4f5-2a23944dab5b	bc78336b-ebbc-4f0e-a1e9-0e01e22eecc5	Зерно	12	12	Мешки
2d43d844-5c06-47dc-8c2f-41729a39d44a	0fcf3342-a095-470b-b876-c05f950ed166	Нефть	12	12	Мешки
5632469d-12dc-44ae-9949-6494a13b0a4a	0b61462a-a9e8-4d5c-8985-58cf7fb0ae32	Нефть	12	12	Мешки
7f88367a-8eb3-45c9-ba2b-2daf923df57b	db3ba060-a87a-4389-9db9-23a46ca1b2c5	Металл	12	12	Мешки
ee5ffc48-1f1a-489c-a3e5-b0b1718248d1	07205a70-5dab-43ab-86f9-42a336f41ce4	Уголь	12	12	Мешки
14530e13-3449-40d6-9d4a-5bdad37d5def	2744c84d-9191-4640-83be-c78cb87e3e87	Уголь	12	12	Мешки
8c0b1309-4d9c-4519-95db-a8e6903728d9	7f294f60-e6b4-49aa-8348-7723006873bd	Нефть	12	12	Мешки
c6fd4ddc-51d8-4601-9e15-27f19b2c0be1	acc641d9-5e8f-4572-97ac-d198cc869f41	Электроника	12	12	Мешки
bc6c7793-9dba-4f7a-82dc-6b0d154b6405	e1ed24b9-1a31-4b2c-a355-3d6615d5177a	Электроника	12	12	Паллеты
6e82e503-3e58-43f1-aea3-64e64c9e604d	0b9eea53-a3bf-4460-ac4e-bf49acc6612e	Электроника	12	12	Ящики
7b7bd89d-1f39-4a3f-b294-76dd74f7e45c	a594b23e-73a5-49e1-957b-dab2fad0348b	Электроника	12	12	Ящики
d84ade8f-ec86-4974-b46c-b548db3803e7	de55172a-63f7-4a5e-b27a-7a76fc1d00b0	Нефть	12	12	Ящики
38c96354-6433-4f5d-9fe9-76088b364a38	37af2a69-a091-4d73-a818-de5b472cdf17	Электроника	1	12	Паллеты
ce7fc3b2-7076-44c0-bd4a-077a385009b5	eb3c7e19-141f-45ef-b5d7-c89f55d96678	Электроника	12	12	Паллеты
1d87d445-b032-470d-aac6-3be66a68912c	7dc85bb2-7f40-4fc1-b48a-cc27200d368f	Электроника	11	11	Контейнеры
1f8c2100-a002-4eef-8f2c-ddc1f83e7f8f	332454bc-6f4c-483c-8428-658087c98491	Уголь	11	11	Контейнеры
eb9dd00a-47f5-43da-9587-f714ca06b3dc	aad222cc-93d5-4a0c-a9a2-9735422d1404	Химия	11	11	Контейнеры
d444809c-d4da-445b-8a4b-6b370949c957	bb45ca59-68f3-4dbb-b0b9-6780cc4987d2	Химия	11	11	Мешки
2ca2f27f-dabf-4aed-afaf-cd90bbb8bab7	3e88d71c-b96e-4e7f-8b84-606ceac160f9	Химия	11	11	Мешки
d30ee880-1e85-4269-9917-893b4d00b83f	d92a8cb1-a72e-4d96-b21a-44937c389fb3	Электроника	11	11	Паллеты
3e8a43f7-5490-40bc-8393-95ac10b1e4d5	5d01e1bf-a827-4a5a-b88c-900a5f80423e	Электроника	11	11	Паллеты
90cc2f5d-d11b-4847-9538-21a2506c495c	a23d2b10-0858-4ffe-acc8-e34a1b66a36b	Электроника	11	11	Паллеты
de4e5b97-b667-4f7b-9be0-3381e53e355c	8ecfc4da-b685-46e5-b140-fc5eccce0283	Лес	11	11	Паллеты
a5e12971-29ad-4657-831f-13c2f4b3d571	4d8ede48-166f-47b4-af01-eeae9bd09317	Лес	11	11	Паллеты
81b19ea5-7aaa-4b88-9782-7daca6a9f023	a5d1bb80-a3d8-4753-b311-b9c54f92031f	Электроника	11	11	Паллеты
2d361f0d-3167-40e5-b7f3-328fd7ef086d	54ca4686-b5b0-4913-b4a5-560accd6f8ec	Электроника	11	11	Паллеты
320ce325-e870-424c-af01-4abc69b46478	acb7c9ae-dd93-402c-b5a4-02d75f225117	Лес	11	1	Паллеты
21497041-2dcf-4f0b-a208-2172e905cfcd	63fd82f6-cdfa-46ce-b8ed-3fb979011139	Лес	11	1	Паллеты
06688eb3-3194-40b1-8b34-a6458779cbd5	3d84139d-42a0-44a1-8f1d-d4e527acc23c	Металл	11	1	Паллеты
2303fc08-4316-4d5a-9ce9-d680d892a374	f102ddf6-496f-4867-a258-535e96b5233e	Электроника	100	1	Бочки
d3772075-3d32-484c-a177-27a2412bb23e	5ac3ae6e-b32d-4bb5-8198-5fa15b3cbee2	Электроника	20	1	Паллеты
5554da00-1297-4824-9e43-cdc7f9fcd356	28cba746-55d8-4d33-935c-4e8a1e90cfef	Электроника	20	1	Паллеты
e6c95559-42c5-43c1-b60d-090379c29a92	2a308525-9afe-4dfa-89dd-135bdda81131	Электроника	111	1	Паллеты
2496ea32-604c-4037-ac3d-d33b76cce78e	cffa32b8-5fe7-4f46-82b4-c7f6bd71b112	Электроника	111	1	Паллеты
09cc7820-9617-4b84-b77c-855a24a6ced7	99ea9615-d2d0-4a9e-ad88-a7cf5e85e119	Электроника	111	1	Паллеты
88c3983a-b3cf-435a-8bf0-5df020468677	56bc0734-7b01-4192-95b2-c86661c8b434	Уголь	25000	60	Цистерна
\.


--
-- Data for Name: company_accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company_accounts (id, inn, company_name, account_number, balance, bik, bank_name, created_at, updated_at, is_main, is_rzd_account) FROM stdin;
dc8d4ffb-d30e-46d5-b757-1e15158af8d1	7701234567	ООО Вектор	40702810123450000001	500000.00	044525225	ПАО СБЕРБАНК	2026-03-01 19:39:34.564182+03	2026-03-01 19:39:34.564182+03	t	f
14cd8705-d4db-4b77-b925-278e2a2f5cff	7701234567	ООО Вектор	40702810765430000002	250000.00	044525111	АО АЛЬФА-БАНК	2026-03-01 19:39:34.564182+03	2026-03-01 19:39:34.564182+03	f	f
72d73118-55ca-4b5d-aaba-762cb7a10d0d	7701234567	ООО Вектор	40702810987650000003	100000.00	044525593	ПАО ВТБ	2026-03-01 19:39:34.564182+03	2026-03-01 19:39:34.564182+03	f	f
15a62129-dd23-4aa8-8254-5cf723351407	7709876543	ЗАО Ромашка	40702810234560000004	750000.00	044525225	ПАО СБЕРБАНК	2026-03-01 19:39:34.565431+03	2026-03-01 19:39:34.565431+03	t	f
c52b7966-5685-48fc-9ac4-b1bac4652435	7709876543	ЗАО Ромашка	40702810765430000005	120000.00	044525111	АО АЛЬФА-БАНК	2026-03-01 19:39:34.565431+03	2026-03-01 19:39:34.565431+03	f	f
bff41842-5493-47f3-bec1-402797fa98e8	1231231231	Крутые ребята	40702810821765680106	49996000.00	044525523	ПАО Сбербанк	2026-03-02 17:28:19.330148+03	2026-03-02 17:28:19.330182+03	f	f
0c487f51-b2ab-42d8-86a9-1a75ece6f58d	1231231231	Крутые ребята	40702810264007386787	49989500.00	044525523	ПАО Сбербанк	2026-03-02 18:10:08.94894+03	2026-03-02 18:10:08.948978+03	f	f
9548a045-d7a3-4eea-9da4-8d4af0c3929e	1231231231	Крутые ребята	40702810468042765354	49995000.00	044525523	ПАО Сбербанк	2026-03-03 17:59:19.572146+03	2026-03-03 17:59:19.5722+03	f	f
b7f9c880-ad87-45fa-92ce-7214503f4004	7708503727	ОАО РЖД (Грузовые перевозки)	40702810900000000001	10019500.00	044525225	ПАО СБЕРБАНК	2026-02-27 18:14:36.036788+03	2026-03-01 19:39:34.561617+03	t	t
\.


--
-- Data for Name: order_services; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_services (id, order_id, service_name, price) FROM stdin;
03e8be62-0803-4aa4-9535-31eb871f84c4	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	страхование	5000.00
689e5495-67db-4c59-a789-2a7637166c8c	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	сопровождение	15000.00
c72281ab-a2b3-4dae-aa2b-7105c5f73217	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	терминальная_обработка	8000.00
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orders (id, user_id, departure_station, destination_station, wagon_id, status, total_price, carbon_footprint_kg, created_at, requested_wagon_type) FROM stdin;
c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11	Москва-Товарная	Екатеринбург	\N	поиск_вагона	\N	\N	2026-02-27 09:47:57.732487+03	КРЫТЫЙ
d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22	Санкт-Петербург	Новосибирск	44444444-4444-4444-4444-444444444444	ожидает_оплаты	150000.00	450.50	2026-02-27 09:47:57.737049+03	КРЫТЫЙ
58335374-87ab-4102-bb42-e7d7b82addca	dd7dbb8a-6559-4f02-b042-a1a2f493fadb	Москва-Товарная	Владивосток	\N	черновик	\N	\N	2026-02-27 14:24:52.069055+03	крытый
4880bfd6-ac26-4a77-b4d8-26427a9e5349	39dfae2c-3187-4204-8ecc-9498836c76d6	а1	а2	\N	черновик	\N	\N	2026-02-27 17:14:57.463908+03	платформа
f27a903a-5004-411f-b719-309ae7dc1ca3	39dfae2c-3187-4204-8ecc-9498836c76d6	Пермь	Пермь 2	\N	черновик	\N	\N	2026-02-27 18:13:40.856343+03	крытый
a184a1a2-1b50-4d45-a208-e9eff8e2e81f	39dfae2c-3187-4204-8ecc-9498836c76d6	ы	ы2	\N	черновик	\N	\N	2026-02-28 15:21:57.642751+03	крытый
0f6c65f2-61db-431a-a896-4667857fe8e6	39dfae2c-3187-4204-8ecc-9498836c76d6	a	a2	\N	черновик	\N	\N	2026-02-28 15:33:15.447802+03	крытый
f93a8445-1db6-4dcd-a51a-a0d56b9117a2	39dfae2c-3187-4204-8ecc-9498836c76d6	q1	q2	\N	черновик	\N	\N	2026-02-28 15:35:08.885786+03	крытый
e8aa5fb0-b731-484a-91d0-05f4ee6366f8	39dfae2c-3187-4204-8ecc-9498836c76d6	12	12	\N	черновик	\N	\N	2026-02-28 15:35:42.406878+03	крытый
bfcbe5a4-2268-4b7c-af15-6c7aec65394d	39dfae2c-3187-4204-8ecc-9498836c76d6	w1	w2	\N	черновик	\N	\N	2026-02-28 15:38:28.205664+03	крытый
b58f7811-98ed-456c-9793-353dacb2ab0b	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 15:43:17.433746+03	крытый
a2e2f0b8-8368-4606-9bd6-b4ad0c3de6fe	39dfae2c-3187-4204-8ecc-9498836c76d6	q1	q2	\N	черновик	\N	\N	2026-02-28 15:47:30.18545+03	крытый
f6a96f76-85d6-45a1-a149-1a70eba3c66b	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 15:50:04.377355+03	крытый
2cfb03fe-376c-4a1a-b613-bbad1b6ef50d	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 15:56:33.87529+03	крытый
2bf4500c-8b8d-491d-a348-4ad88a696ec2	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 16:07:07.620717+03	крытый
0cf26f67-8a08-4f69-873e-ca112939674f	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 16:09:30.563278+03	крытый
f60a6ce4-5099-47b6-9fbd-3d62224a75e6	39dfae2c-3187-4204-8ecc-9498836c76d6	й1	й2	\N	черновик	\N	\N	2026-02-28 17:21:33.928414+03	крытый
9abd9cdc-a3e1-476c-838e-5cbfa255886f	39dfae2c-3187-4204-8ecc-9498836c76d6	q1	q2	\N	черновик	\N	\N	2026-02-28 17:22:23.813011+03	крытый
36c9524e-8a1b-428c-9409-69c4c89accdc	39dfae2c-3187-4204-8ecc-9498836c76d6	asfsfa	fsa	\N	черновик	\N	\N	2026-02-28 17:24:13.121061+03	крытый
be084036-ac97-47d6-9b9a-7b635d8fb53c	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	Пермь	\N	черновик	\N	\N	2026-02-28 17:48:29.619362+03	крытый
5f9c941d-82cb-46a6-80df-6d689c3563a8	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	Пермь	\N	черновик	\N	\N	2026-02-28 17:54:38.381933+03	крытый
340a7460-7b58-4d9f-b22e-f3f865b0fa78	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	Пермь	\N	черновик	\N	\N	2026-02-28 17:55:22.165383+03	крытый
ee90a8ca-9900-45be-8eac-7b1472a1593c	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	Пермь	\N	черновик	\N	\N	2026-02-28 17:55:28.474117+03	крытый
1f10acaf-fcf0-46ae-a28b-d3bc50364338	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-товарная	Пермь	\N	черновик	\N	\N	2026-02-28 17:55:52.097365+03	крытый
92b3c5d7-3d3e-4063-a448-f7afd6946820	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-02-28 17:56:01.471321+03	полувагон
a91edb6a-7177-455a-ad44-c82b1aee035e	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	а	\N	черновик	\N	\N	2026-02-28 18:03:05.448093+03	крытый
ee6d61bd-c741-40b8-82f8-e81312a1b37b	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	а	\N	черновик	\N	\N	2026-02-28 18:03:07.687248+03	крытый
93a28763-e8fc-42a2-97f9-d836d2e29063	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	а	\N	черновик	\N	\N	2026-02-28 18:03:09.491601+03	крытый
fbd8a73d-4c5b-4fc2-873a-4d46a7675458	39dfae2c-3187-4204-8ecc-9498836c76d6	Казань	а	\N	черновик	\N	\N	2026-02-28 18:03:11.53795+03	крытый
1e9e9925-fec5-4b84-8d83-e99838bd95ce	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	а	\N	черновик	\N	\N	2026-02-28 18:03:18.26925+03	крытый
dcbd8e1c-8951-4ead-9cdb-a3419d487fe2	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-02-28 19:03:08.453903+03	крытый
c05591be-92c0-419d-b969-c6a1ff144cc8	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 12:31:41.750503+03	крытый
4088f0d8-7f4b-484f-ae91-cb8fa4cb8559	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 12:31:44.865549+03	полувагон
484131f3-7bcc-4e44-a3f1-2e88010889fe	39dfae2c-3187-4204-8ecc-9498836c76d6	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 12:32:38.028195+03	полувагон
e8886610-6f86-40ac-81f8-67a8258e62ba	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:19:58.788476+03	полувагон
35c50ce5-acc4-44e6-b5e8-6bc7bb1fbb8d	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:20:01.751971+03	полувагон
15c5ce61-145e-47d3-9fdf-54b906da65af	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:20:04.537689+03	крытый
46d29d04-69bc-4ed3-b976-7776f5ae3293	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:20:06.637621+03	крытый
c6fcfb05-496c-4c0e-b81c-e21e37eed19d	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:20:09.420365+03	полувагон
ecc023af-3c74-4d0d-97a9-ef6464026b48	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 13:20:11.508562+03	цистерна
23ccc339-e2d8-4789-8885-6aa68c6fce91	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:05:51.584988+03	крытый
98ef3689-39a3-4885-8182-108de5857b97	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:05:56.340631+03	крытый
1e5dd479-62cf-4019-a4b4-5638f3d1471d	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:05:58.601857+03	крытый
404e1650-a79a-4983-98b7-824cee346636	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:01.09425+03	полувагон
c14ee064-c29f-48fc-a2e8-65320aa32e27	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:02.674001+03	цистерна
55a38478-c0a0-4848-85c2-6fd5a1ea5e90	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:09.021297+03	полувагон
45cd68e6-c1a9-42b7-8bf0-c04ab1b0fd48	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:10.919224+03	крытый
a6e08faa-f073-403e-a6e6-e6b6c9eccb59	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:14.086188+03	полувагон
3f2201d0-69d3-42a9-bf57-7370d0671907	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:16.317875+03	цистерна
d7382b47-61a6-4ab5-9e8c-717d8029e71c	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:18.739036+03	платформа
1e1cc3c7-5e14-4590-ac2a-5dcc8e7dd90d	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:21.613215+03	платформа
aaceb290-ed3e-48af-ba7b-9df8ebb82365	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:23.193916+03	крытый
fcb6ee71-a07b-4bf6-812b-577522ddaef2	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:25.275905+03	крытый
ff5653ce-5024-45a6-a143-feeac30abd81	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:28.747799+03	крытый
7e3060ef-1db0-49a6-aa44-344b0528df19	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:30.803437+03	полувагон
9f5ea48e-be24-4366-b4f7-d703eddc4b8e	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:32.361325+03	полувагон
ebc31a45-48eb-46ec-9fec-c7b65e443fb1	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:35.610936+03	полувагон
b84a5336-b5b3-4b3f-8a33-607224a5b102	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:37.307656+03	полувагон
5a5214b0-d87c-461e-b419-4acf8348100b	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:39.235787+03	полувагон
8406ed16-9cd6-4e56-8f5f-140964c6f1a4	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:40.908911+03	полувагон
28e76677-aac9-47a2-93da-611851970eaa	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:42.776987+03	полувагон
d4c5f1a4-a001-4c51-a360-8338af3c3cda	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:44.50817+03	полувагон
2f63ccb3-05a8-4ad6-8859-e472931433e4	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:47.050244+03	полувагон
cbecae02-f6d0-40e1-8352-1ea33fc45630	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:50.133025+03	цистерна
1eb2715b-c77c-4888-ae88-2710c71cd0d3	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:51.778809+03	платформа
bc78336b-ebbc-4f0e-a1e9-0e01e22eecc5	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:06:53.378717+03	рефрижератор
0fcf3342-a095-470b-b876-c05f950ed166	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:14.369899+03	рефрижератор
0b61462a-a9e8-4d5c-8985-58cf7fb0ae32	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:24.265925+03	крытый
db3ba060-a87a-4389-9db9-23a46ca1b2c5	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:26.576304+03	крытый
07205a70-5dab-43ab-86f9-42a336f41ce4	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:28.679236+03	крытый
2744c84d-9191-4640-83be-c78cb87e3e87	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:30.749475+03	крытый
7f294f60-e6b4-49aa-8348-7723006873bd	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:32.33787+03	крытый
acc641d9-5e8f-4572-97ac-d198cc869f41	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:34.176098+03	крытый
e1ed24b9-1a31-4b2c-a355-3d6615d5177a	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:35.751239+03	крытый
0b9eea53-a3bf-4460-ac4e-bf49acc6612e	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:37.141565+03	крытый
a594b23e-73a5-49e1-957b-dab2fad0348b	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:38.721402+03	полувагон
de55172a-63f7-4a5e-b27a-7a76fc1d00b0	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:07:41.752261+03	полувагон
37af2a69-a091-4d73-a818-de5b472cdf17	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 15:09:50.002812+03	рефрижератор
eb3c7e19-141f-45ef-b5d7-c89f55d96678	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:18:34.468045+03	рефрижератор
7dc85bb2-7f40-4fc1-b48a-cc27200d368f	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:29:23.886276+03	платформа
332454bc-6f4c-483c-8428-658087c98491	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:29:27.32106+03	платформа
aad222cc-93d5-4a0c-a9a2-9735422d1404	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:29:33.691571+03	платформа
bb45ca59-68f3-4dbb-b0b9-6780cc4987d2	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:29:38.71146+03	платформа
3e88d71c-b96e-4e7f-8b84-606ceac160f9	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 17:29:41.826856+03	платформа
d92a8cb1-a72e-4d96-b21a-44937c389fb3	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 18:07:06.803896+03	крытый
5d01e1bf-a827-4a5a-b88c-900a5f80423e	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 18:07:13.57961+03	рефрижератор
a23d2b10-0858-4ffe-acc8-e34a1b66a36b	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-01 18:07:18.073813+03	рефрижератор
8ecfc4da-b685-46e5-b140-fc5eccce0283	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	\N	поиск_вагона	\N	\N	2026-03-01 18:08:48.328281+03	полувагон
4d8ede48-166f-47b4-af01-eeae9bd09317	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-02 17:17:56.314743+03	платформа
a5d1bb80-a3d8-4753-b311-b9c54f92031f	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Санкт-Петербург	Пермь	4b5c6265-a0f5-4827-a147-6f34075111c8	ожидает_оплаты	10000.00	\N	2026-03-02 17:18:17.711784+03	крытый
54ca4686-b5b0-4913-b4a5-560accd6f8ec	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Санкт-Петербург	Пермь	\N	черновик	\N	\N	2026-03-02 17:23:19.594851+03	крытый
acb7c9ae-dd93-402c-b5a4-02d75f225117	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Новосибирск	Пермь	\N	черновик	\N	\N	2026-03-02 17:24:53.775206+03	платформа
63fd82f6-cdfa-46ce-b8ed-3fb979011139	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	\N	черновик	\N	\N	2026-03-02 17:27:36.737777+03	платформа
28cba746-55d8-4d33-935c-4e8a1e90cfef	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	ожидает_оплаты	5000.00	\N	2026-03-03 17:49:08.489299+03	крытый
3d84139d-42a0-44a1-8f1d-d4e527acc23c	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Москва-Товарная	Пермь	f5e4d3c2-2222-5b6c-9d0e-1f2a3b4c5d6e	оплачен	4000.00	\N	2026-03-02 17:27:47.944397+03	полувагон
f102ddf6-496f-4867-a258-535e96b5233e	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Самара	а	c3d4e5f6-4444-7d8e-1f2a-3b4c5d6e7f8a	оплачен	10500.00	\N	2026-03-02 18:09:43.233619+03	цистерна
5ac3ae6e-b32d-4bb5-8198-5fa15b3cbee2	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	\N	черновик	\N	\N	2026-03-03 17:25:06.044701+03	крытый
2a308525-9afe-4dfa-89dd-135bdda81131	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	ожидает_оплаты	5000.00	\N	2026-03-03 17:50:07.974706+03	крытый
cffa32b8-5fe7-4f46-82b4-c7f6bd71b112	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	ожидает_оплаты	5000.00	\N	2026-03-03 17:55:20.775059+03	крытый
99ea9615-d2d0-4a9e-ad88-a7cf5e85e119	bbd7e5d6-dd22-4cfa-a650-92077fabec72	Казань	Пермь	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	оплачен	5000.00	\N	2026-03-03 17:59:06.190254+03	крытый
56bc0734-7b01-4192-95b2-c86661c8b434	dd7dbb8a-6559-4f02-b042-a1a2f493fadb	Москва-Товарная	Владивосток	\N	черновик	\N	\N	2026-03-03 19:21:44.863445+03	крытый
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (id, order_id, payment_id, amount, status, payment_method, company_name, inn, kpp, bik, account_number, correspondent_account, bank_name, payment_purpose, payment_document, payment_date, created_at, paid_at, error_message, metadata) FROM stdin;
5e32130f-47bc-4665-b9e2-aa8ad670b899	d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44	pay_123456789	150000.00	SUCCEEDED	BANK_TRANSFER	ООО Ромашка	7701234567	770101001	044525225	40702810123450123456	\N	ПАО СБЕРБАНК	Оплата грузовой перевозки по договору №РЖД-2026-123	РЖД-1712345678-123	\N	2026-02-27 10:00:00+03	\N	\N	\N
26c306e3-6d74-445d-82d7-4f3ba74c51c9	c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33	pay_987654321	75000.00	PENDING	BANK_TRANSFER	ООО Вектор	7709876543	770901001	044525111	40702810567890123456	\N	АО АЛЬФА-БАНК	Предоплата за перевозку оборудования	РЖД-1712345678-124	\N	2026-02-27 11:30:00+03	\N	\N	\N
978f6496-84bc-4673-a845-d16f85313f51	99ea9615-d2d0-4a9e-ad88-a7cf5e85e119	pay_1db4abc6bd894e3a	5000.00	SUCCEEDED	\N	Крутые ребята	1231231231	\N	044525523	40702810468042765354		ПАО Сбербанк	р	РЖД-1772549959-650	2026-03-03 17:59:19.656726+03	2026-03-03 17:59:19.658118+03	2026-03-03 17:59:19.656762+03	\N	Перевод со счета 40702810468042765354 (баланс был: 50000000,00, стал: 49995000,00) на счет РЖД 40702810900000000001 (баланс был: 10014500,00, стал: 10019500,00). Сумма: 5000,00
\.


--
-- Data for Name: station_distances; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.station_distances (id, from_station, to_station, distance_km, updated_at) FROM stdin;
99dbcd6a-695e-4790-ab10-d81975961a4e	Москва-Товарная	Санкт-Петербург-Товарный	650	2026-02-28 14:45:27.857453+03
eea1422d-b57d-48ff-a06b-f4ba8135e90f	Москва-Товарная	Екатеринбург-Товарный	1667	2026-02-28 14:45:27.857453+03
c08a4718-2cdd-46a0-9713-0b7a90479149	Москва-Товарная	Новосибирск-Товарный	2811	2026-02-28 14:45:27.857453+03
a463c945-1082-434d-9964-018956de3f39	Москва-Товарная	Казань-Товарная	797	2026-02-28 14:45:27.857453+03
ef11f9ba-f500-4824-a92e-c54fcbb5da05	Санкт-Петербург-Товарный	Москва-Товарная	650	2026-02-28 14:45:27.857453+03
2bc6dde9-26a4-430e-a1e5-0a395e50dc45	Санкт-Петербург-Товарный	Мурманск-Товарный	1240	2026-02-28 14:45:27.857453+03
993c99d7-8543-42fc-8a9e-dba283fd1082	Екатеринбург-Товарный	Москва-Товарная	1667	2026-02-28 14:45:27.857453+03
7d906e72-f6e8-4c42-bbb4-a208ea8fb960	Екатеринбург-Товарный	Новосибирск-Товарный	1520	2026-02-28 14:45:27.857453+03
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariffs (id, cargo_type, wagon_type, base_rate, coefficient, description, created_at, updated_at) FROM stdin;
f4e008ba-52da-4e19-b44b-ba192d6d1555	Электроника	крытый	15.50	1.20	Тариф для электроники в крытых вагонах	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
27e7320d-0d32-4afe-aa9c-db0fe2048c67	Электроника	полувагон	14.00	1.10	Тариф для электроники в полувагонах	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
85bed36b-5eaf-44ac-afbf-61a102098ece	Уголь	полувагон	8.50	1.00	Тариф для угля	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
679e2f3c-3032-43c0-b2b7-fd946fa8a575	Нефть	цистерна	12.00	1.30	Тариф для нефтепродуктов	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
c0102dfb-f740-45c3-b0af-dd4ebe7316aa	Металл	платформа	11.50	1.10	Тариф для металлопроката	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
4eb9c0bb-7b58-4fbe-b4a1-4dd6b7c0ecbd	Лес	полувагон	9.00	1.00	Тариф для леса	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
d570b769-843f-4673-9da2-f75ba512d81f	Оборудование	крытый	18.00	1.40	Тариф для оборудования	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
996b2524-8021-4748-931d-d0d3df5e12de	Зерно	крытый	7.50	0.90	Тариф для зерна	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
bafe1b55-132b-4619-8a9b-d7a9549f7368	Химия	цистерна	13.50	1.50	Тариф для химических грузов	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
723f2e4d-d378-4b52-9796-f514df508a28	Контейнеры	платформа	16.00	1.20	Тариф для контейнеров	2026-02-27 09:47:57.741636+03	2026-02-27 09:47:57.741636+03
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, company_name, inn, created_at, role) FROM stdin;
a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11	logistics@vector.ru	hashed_pwd_1	ООО Вектор	7701234567	2026-02-27 09:47:57.727681+03	USER
b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22	supply@romashka.ru	hashed_pwd_2	ЗАО Ромашка	7709876543	2026-02-27 09:47:57.727681+03	USER
9c9f92ce-4338-4097-bfee-43da34e5ee53	client@logistic.ru	$2a$10$bu/B6EMAJKB0mxCcFQTlCecELHCUAv6mejfBwjETSe5fa0YJxuI06	ООО ТрансЛогистик	7712345678	2026-02-27 10:55:46.398109+03	USER
dd7dbb8a-6559-4f02-b042-a1a2f493fadb	test100@logistic.ru	$2a$10$CjextjFyljbSfkYa8dzEBOWxLlmMKBFP6m9aEn6yUpohTDTTuazjK	ООО Тест	7712345678	2026-02-27 11:05:50.299956+03	USER
39dfae2c-3187-4204-8ecc-9498836c76d6	asd@asd.asd	$2a$10$p9u4mxsiL7pzMJsd6/8DhuUiz/Ggufg.8R5KRJ2SfUK4aJvrmat2y	OOO "Компания"	4723494089124	2026-02-27 17:08:15.260668+03	USER
fd12cbed-6bb8-453e-a76d-7db661462ce7	asd@asd.as	$2a$10$VLXuqfqmL/JmMVfCoT2YkOseyUzP/MVU7nPHBh6vHMyltIAWhuqx2	у	23	2026-03-01 12:39:28.163091+03	USER
bbd7e5d6-dd22-4cfa-a650-92077fabec72	hello@hello.hello	$2a$10$ZbJ0/SXGMNsNca22sSDSHuUU1FiUbbv1DO08st5z3T5Aqq1cOtq8S	Крутые ребята	1231231231	2026-03-01 13:18:58.378277+03	USER
\.


--
-- Data for Name: wagon_schedule; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagon_schedule (id, wagon_id, order_id, departure_station, arrival_station, departure_date, arrival_date, status, cargo_type, cargo_weight_kg, created_at) FROM stdin;
4e47664d-b969-403b-80e7-9cbb7207ee46	33333333-3333-3333-3333-333333333333	be084036-ac97-47d6-9b9a-7b635d8fb53c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 17:48:53.97936+03
d41d3ab0-8598-4fc5-b8af-adfdcd19cb4d	ae2f83cb-ca42-468c-b3cc-4e7eadd0e451	92b3c5d7-3d3e-4063-a448-f7afd6946820	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 17:57:05.716814+03
468b0536-ac3f-4978-af94-fd93fbbc6849	11111111-1111-1111-1111-111111111111	1e9e9925-fec5-4b84-8d83-e99838bd95ce	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 18:04:46.144226+03
12316afe-72cb-4c71-9d73-4e65e919ef9d	dfcdf6bd-c02d-4cd2-8a78-65bbc67cb9a1	dcbd8e1c-8951-4ead-9cdb-a3419d487fe2	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-02-28 19:03:16.168761+03
3ae4641e-afaf-414b-bc8b-332b830a5db4	22222222-2222-2222-2222-222222222222	484131f3-7bcc-4e44-a3f1-2e88010889fe	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 12:32:44.860301+03
b0b2c6a1-9850-4f04-93ce-8f07cb9ca4af	e8aae1c3-477e-4f80-98bc-b0aab4f9c8c2	ecc023af-3c74-4d0d-97a9-ef6464026b48	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 13:20:23.997161+03
f0644a8a-40e0-4b47-8dc4-24c3a0033d99	0df5e622-37b1-433b-96bd-10639863f645	eb3c7e19-141f-45ef-b5d7-c89f55d96678	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 17:18:37.685885+03
6b6fe294-af24-4656-809e-d6db5c0b02fa	cc6e89b6-7014-4abf-bc97-95c21187476c	8ecfc4da-b685-46e5-b140-fc5eccce0283	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-01 18:08:59.071409+03
e4358e3b-fe6a-4ff0-8365-0e7c75803468	4b5c6265-a0f5-4827-a147-6f34075111c8	a5d1bb80-a3d8-4753-b311-b9c54f92031f	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-02 17:18:22.216329+03
8a951d49-2bd0-421a-b903-52ceeabbc729	f5e4d3c2-2222-5b6c-9d0e-1f2a3b4c5d6e	3d84139d-42a0-44a1-8f1d-d4e527acc23c	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-02 17:27:55.407006+03
3af81963-2cd1-4ad7-a221-d3f3e5707735	c3d4e5f6-4444-7d8e-1f2a-3b4c5d6e7f8a	f102ddf6-496f-4867-a258-535e96b5233e	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-02 18:09:55.397062+03
862d179f-0ba0-4e81-9d2c-bb1300acf626	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	28cba746-55d8-4d33-935c-4e8a1e90cfef	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-03 17:49:12.226394+03
0eebe20c-e61b-4e3f-9a39-774beeb0cd1b	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	2a308525-9afe-4dfa-89dd-135bdda81131	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-03 17:50:14.88758+03
186bdcdd-2e4e-47b5-9685-3dedd95f7189	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	cffa32b8-5fe7-4f46-82b4-c7f6bd71b112	ожидает	ожидает	\N	\N	отменен	\N	\N	2026-03-03 17:55:25.288358+03
111b814c-d1e5-436c-a7cf-5c0cd9911f9f	e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	99ea9615-d2d0-4a9e-ad88-a7cf5e85e119	ожидает	ожидает	\N	\N	зарезервирован	\N	\N	2026-03-03 17:59:10.078442+03
\.


--
-- Data for Name: wagon_tariffs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagon_tariffs (id, wagon_type, cargo_type, base_rate_per_km, coefficient, min_price, description) FROM stdin;
b0c54401-4dcb-4d3b-b6ac-7433ea6a6660	крытый	Электроника	15.50	1.20	5000.00	\N
9393c72a-6b29-4b57-872b-37f25e55e19b	крытый	Оборудование	18.00	1.40	6000.00	\N
25f7de7b-ec97-49a2-8d4e-f67eb74c32ad	крытый	Металл	12.00	1.10	4500.00	\N
515ea962-642c-4f91-9fc5-d2062d5ed2eb	крытый	Уголь	10.00	1.00	3500.00	\N
0e4d49e7-5e14-46f3-92b0-1c218e17663e	крытый	Нефть	13.00	1.20	5000.00	\N
4bf41d08-57d6-40d7-afdf-0964025507fe	крытый	Лес	11.00	1.00	4000.00	\N
44c5583d-9e42-4e1e-9194-6f30bcf84fc1	крытый	Зерно	9.50	0.90	3500.00	\N
b0c95f39-003c-4620-976e-72e5aeb46364	крытый	Химия	14.00	1.30	5500.00	\N
95f11195-44b6-4bcf-8ff7-bfb5524be91e	крытый	Контейнеры	12.50	1.00	4500.00	\N
afac4151-08df-40f4-93fb-47844eddaef4	крытый	Трубы_стальные	12.50	1.10	4500.00	\N
519c2ea4-0893-4975-921b-cdc901bde3b1	крытый	Стандартный	14.00	1.00	4000.00	\N
ec5993b3-dc66-485e-a7c5-3579839f3b63	крытый	Сыпучий	10.50	1.00	3500.00	\N
d302a504-8c37-463e-84c6-e57a4cfd6868	крытый	Наливной	13.50	1.20	5000.00	\N
332946d2-63e5-4d79-9876-ab878f5dbeee	крытый	Опасный	16.00	1.50	7000.00	\N
6846900d-91a6-471d-b130-8766a18b32a3	крытый	Хрупкий	17.00	1.30	6000.00	\N
6e7b07a0-3cae-4a8c-8fc3-b793b6879dea	крытый	Негабаритный	19.00	1.60	8000.00	\N
a0fb02c2-bef6-4114-93a1-f6003c90fd66	крытый	общий	14.00	1.00	4000.00	\N
f85b21e4-c554-40d1-9399-0b7fae80c926	полувагон	Уголь	8.50	1.00	3500.00	\N
3f6491cb-ed62-4237-a302-c219a8dc0b1c	полувагон	Металл	9.50	1.10	4000.00	\N
acee955a-b568-4cc5-9e97-407330d922f0	полувагон	Лес	9.00	1.00	3800.00	\N
782ed72b-e23e-4773-ad56-b18019c16055	полувагон	Руда	8.80	1.10	3500.00	\N
54bc2d36-5b8d-4085-bc0f-02cff6c2994f	полувагон	Щебень	7.50	0.90	3000.00	\N
32d3fef9-ff4f-4a92-9199-7f0d673a7618	полувагон	Зерно	8.00	0.90	3200.00	\N
d0397951-ec22-44b2-98ce-efdbceb6d578	полувагон	Оборудование	10.50	1.20	4500.00	\N
0cb16945-90db-4e09-bbf5-53cdaef696a2	полувагон	Электроника	11.00	1.20	4500.00	\N
fc98e963-d521-48fc-88d9-588f834b176a	полувагон	Нефть	9.50	1.10	4000.00	\N
c91993e8-9ecf-453e-a1e7-81d0aa3f6fe3	полувагон	Химия	10.00	1.20	4200.00	\N
044cc3a1-8270-4570-ab8f-865798897a8a	полувагон	Контейнеры	9.80	1.00	3800.00	\N
e24593ec-440b-44b7-9e30-6999b5c5d281	полувагон	Трубы_стальные	9.20	1.10	4000.00	\N
99e60070-04ff-4103-a8e1-c87bd5d3bd72	полувагон	Стандартный	9.00	1.00	3500.00	\N
800f3d59-3411-4fb0-a3ff-61b6903d9642	полувагон	Сыпучий	8.00	1.00	3000.00	\N
1a1f340f-ca72-4b08-823a-4e72162bc629	полувагон	Наливной	9.50	1.10	4000.00	\N
03d78855-8dc4-4530-8078-a0d25a8350bc	полувагон	Опасный	11.00	1.40	5000.00	\N
09c8c42a-2170-4d30-aed9-1c984ee011e3	полувагон	Хрупкий	12.00	1.30	4500.00	\N
6dea9ef2-1331-4d32-a102-d0b750a6bbd3	полувагон	Негабаритный	13.00	1.50	5500.00	\N
6bcb4b50-f1da-4bf0-a937-4ed91b5f4416	полувагон	общий	8.50	1.00	3500.00	\N
51ed90fb-9182-45ce-a785-a789875dc60e	платформа	Контейнеры	11.00	1.00	4500.00	\N
3f3d2a9e-f9e2-4967-9dcc-314562bd39c0	платформа	Металл	11.50	1.10	5000.00	\N
972dab4b-17c6-4d7e-8670-aeeb75fb252a	платформа	Оборудование	14.00	1.30	6000.00	\N
c77b623f-fade-4cd2-8ec4-775efbd0b60e	платформа	Лес	10.50	1.00	4000.00	\N
b78d0902-a1f7-4b3b-8737-38c5c2fce3bb	платформа	Трубы_стальные	11.00	1.10	4500.00	\N
c572d6e2-e024-4543-b412-6a1aaca776d3	платформа	Уголь	9.00	1.00	3500.00	\N
e2bed906-27f6-487b-8e8f-369ed967e385	платформа	Нефть	10.00	1.10	4000.00	\N
b53a47f5-6e15-4c7e-a2f5-0c0809a3b3ac	платформа	Химия	11.50	1.20	4800.00	\N
b77158b4-7f12-44e5-a610-c3053d7a6136	платформа	Электроника	12.00	1.20	5000.00	\N
c31a233a-f3e0-4533-8a64-aa662a0e6041	платформа	Зерно	8.50	0.90	3200.00	\N
17eecbf3-587c-4788-8641-6a71accf991b	платформа	Стандартный	10.00	1.00	3500.00	\N
1a969cb8-d0e1-4b08-a870-63e57b6424ff	платформа	Сыпучий	9.00	1.00	3300.00	\N
0b66c0b8-69e9-4315-a334-784ea4dbfe44	платформа	Наливной	10.50	1.10	4000.00	\N
781057b5-28be-45af-ac36-59afcd95ebdc	платформа	Опасный	13.00	1.40	5500.00	\N
5554869e-10b0-42e4-a6fe-099b73955abb	платформа	Хрупкий	14.00	1.30	5000.00	\N
9b7f7f8a-bacf-480a-a487-b7e56f810619	платформа	Негабаритный	15.00	1.50	6500.00	\N
4c828109-fbbd-4375-82a6-e5a35bc4b4a3	платформа	общий	10.00	1.00	3500.00	\N
e067bacc-1397-49e7-a5c4-cca7b8636dfd	цистерна	Нефть	14.00	1.30	6000.00	\N
90e32b8a-cbdf-4b0d-86c2-f6a7eb20239f	цистерна	Химия	13.50	1.50	6500.00	\N
ab7a648b-ed27-491b-b53b-b2a156d3027e	цистерна	Металл	12.00	1.20	5500.00	\N
cca55265-adb7-4bfe-8d9f-9b01630ca60d	цистерна	Бензин	14.50	1.40	6000.00	\N
d43fe390-77d6-4505-a49f-24111a908138	цистерна	Газ	16.00	1.60	7000.00	\N
5e424a61-fca4-4953-bfd4-805218abbc0a	цистерна	Молоко	13.00	1.20	5500.00	\N
a8de5269-69d6-4e3c-9fc1-f46c125ebbb4	цистерна	Спирт	15.00	1.50	6500.00	\N
74e5500d-5f54-4789-826a-1d9e6e36119f	цистерна	Кислота	14.50	1.60	7000.00	\N
86d7e518-9bfe-4f12-acd1-6595abea3686	цистерна	Вода	11.00	1.00	4500.00	\N
8617e9cb-4046-42fd-95a6-bda525f3e898	цистерна	Пиво	13.00	1.20	5500.00	\N
f5ab47cf-eba3-4d12-8c69-6d8bed3e1fa2	цистерна	Оборудование	12.50	1.20	5000.00	\N
948063f6-dd6f-4616-a848-33b9d6a3acdf	цистерна	Электроника	13.00	1.20	5500.00	\N
232f11fb-ced9-4852-bafd-90da1fe54cfc	цистерна	Уголь	10.00	1.00	4000.00	\N
199444e9-a738-4133-90ef-ba0bacb38dbb	цистерна	Лес	9.50	1.00	3800.00	\N
2418187a-75ba-4188-a357-afff297569d7	цистерна	Зерно	9.00	0.90	3500.00	\N
940f5e62-53ca-4a27-8ce7-92cb671e9147	цистерна	Контейнеры	11.00	1.00	4500.00	\N
769951d3-884b-4a87-a01d-b4d47ea0a782	цистерна	Трубы_стальные	11.50	1.10	5000.00	\N
90c255d1-c041-4c8e-b95a-949e9ebeb881	цистерна	Стандартный	12.00	1.00	5000.00	\N
ff47cc43-fc8a-4ce7-ada5-2e87f7ea61de	цистерна	Сыпучий	10.00	1.00	4000.00	\N
9ccbb45b-0a4d-418e-a61a-97dc268eb00f	цистерна	Наливной	14.00	1.30	6000.00	\N
231b91d2-fed6-4aa8-ae1e-2a6cb353d173	цистерна	Опасный	15.00	1.50	7000.00	\N
e270bdb7-d202-4fe5-981d-779eb850afae	цистерна	Хрупкий	16.00	1.40	6500.00	\N
85ed7ef8-30c6-400e-bab9-4f881a32838e	цистерна	Негабаритный	17.00	1.60	7500.00	\N
d68b54db-0bc9-4622-9eba-dd56733a851c	цистерна	общий	12.00	1.00	5000.00	\N
363d1538-fc62-4e9c-8da6-8bd40b0efc67	рефрижератор	Продукты	22.00	1.40	8000.00	\N
c252492e-e53d-4170-b7a2-ad3fbeea1f2a	рефрижератор	Медикаменты	25.00	1.60	10000.00	\N
54a15841-9e98-41bb-861b-3e0b679177cf	рефрижератор	Цветы	24.00	1.50	9000.00	\N
463a1ec5-c9b7-4f94-aaf0-93877b518fce	рефрижератор	Мясо	23.00	1.40	8500.00	\N
cd661e16-f557-44ca-b92c-453f52ca2bce	рефрижератор	Рыба	23.00	1.40	8500.00	\N
aa99555b-a96c-4106-8a77-d945f2e33d14	рефрижератор	Овощи	21.00	1.30	7500.00	\N
e6aa3bfb-a6e8-400c-8d17-3ab4538946d7	рефрижератор	Фрукты	22.00	1.30	7800.00	\N
c84e87c4-35d6-486e-91b2-b01c2436cfe7	рефрижератор	Мороженое	24.00	1.50	9000.00	\N
4d114d00-2fa4-4af9-93e5-da44272c3242	рефрижератор	Вакцины	26.00	1.70	11000.00	\N
302f4fd9-570e-4982-b044-fe87d76ae95c	рефрижератор	Химия	20.00	1.40	8000.00	\N
8e328be2-06ed-4358-80cf-fe30906cef09	рефрижератор	Оборудование	19.00	1.30	7000.00	\N
103533cf-78f4-40a6-871e-44149f49e411	рефрижератор	Электроника	20.00	1.30	7500.00	\N
21334bb9-fe1a-4426-aeb3-16f5231d7afb	рефрижератор	Металл	18.00	1.20	6500.00	\N
c7d4de94-ca3d-4c40-a9d2-f077f08d168a	рефрижератор	Уголь	15.00	1.00	5500.00	\N
f2b269fb-3f64-40cb-83d2-09880be14fc6	рефрижератор	Нефть	17.00	1.20	6000.00	\N
ae02e4ad-8b80-42f7-84be-b6bb28c3aad1	рефрижератор	Лес	16.00	1.00	5500.00	\N
4a12d764-b590-4761-b4ce-a003f2699995	рефрижератор	Зерно	15.00	1.00	5000.00	\N
c27eb7cc-b1b5-4d72-bdcf-fd300c725dc4	рефрижератор	Контейнеры	18.00	1.10	6500.00	\N
d703ee0f-ff06-4e4a-8c49-59de92ccee69	рефрижератор	Трубы_стальные	17.00	1.10	6000.00	\N
cfe43aa9-f533-43ba-b568-c7175d82e9cc	рефрижератор	Стандартный	20.00	1.00	7000.00	\N
8bcf52d7-614f-4f7f-87b5-cb59f79d12c2	рефрижератор	Сыпучий	16.00	1.00	5500.00	\N
0eb464af-bde5-4f65-916c-0bacbb930329	рефрижератор	Наливной	18.00	1.20	6500.00	\N
e0552516-5d62-4352-b651-fba512271837	рефрижератор	Опасный	22.00	1.50	9000.00	\N
5a17159c-6b2f-4f04-ae8b-ab037760b226	рефрижератор	Хрупкий	23.00	1.40	8500.00	\N
6046fc4e-12ce-4900-8617-4e15365aa12f	рефрижератор	Негабаритный	24.00	1.60	9500.00	\N
0b8a3265-3f95-42f5-a208-e52c6dc67ff7	рефрижератор	общий	20.00	1.00	7000.00	\N
\.


--
-- Data for Name: wagons; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wagons (id, wagon_number, wagon_type, max_weight_kg, max_volume_m3, current_station, status) FROM stdin;
44444444-4444-4444-4444-444444444444	55667788	платформа	72000	0	Санкт-Петербург	забронирован
15bb73e5-010b-472c-94fc-f560247469d0	77777777	платформа	75000	0	Москва-Товарная	свободен
50b53689-ccb2-4197-b3eb-4778c064e11a	22222222	платформа	72000	0	Новосибирск	свободен
33333333-3333-3333-3333-333333333333	11223344	крытый	68000	120	Казань	забронирован
ae2f83cb-ca42-468c-b3cc-4e7eadd0e451	88888888	полувагон	72000	90	Москва-Товарная	забронирован
11111111-1111-1111-1111-111111111111	12345678	крытый	68000	120	Москва-Товарная	забронирован
dfcdf6bd-c02d-4cd2-8a78-65bbc67cb9a1	99999999	крытый	70000	140	Москва-Товарная	забронирован
22222222-2222-2222-2222-222222222222	87654321	полувагон	71000	85	Москва-Товарная	забронирован
e8aae1c3-477e-4f80-98bc-b0aab4f9c8c2	66666666	цистерна	68000	80	Москва-Товарная	забронирован
0df5e622-37b1-433b-96bd-10639863f645	55555555	рефрижератор	65000	110	Москва-Товарная	забронирован
cc6e89b6-7014-4abf-bc97-95c21187476c	33333333	полувагон	71000	85	Казань	забронирован
4b5c6265-a0f5-4827-a147-6f34075111c8	44444444	крытый	68000	120	Санкт-Петербург	забронирован
a1b2c3d4-1111-4a5b-8c9d-0e1f2a3b4c5d	50000001	крытый	68000	120	Екатеринбург-Сортировочный	свободен
b2c3d4e5-3333-6c7d-0e1f-2a3b4c5d6e7f	50000003	платформа	72000	0	Санкт-Петербург	свободен
d4e5f6a1-5555-8e9f-2a3b-4c5d6e7f8a9b	50000005	рефрижератор	65000	110	Новосибирск	свободен
f6a1b2c3-7777-0a1b-4c5d-6e7f8a9b0c1d	50000007	полувагон	70000	88	Ростов-Главный	свободен
a1b2c3d4-8888-1b2c-5d6e-7f8a9b0c1d2e	50000008	платформа	75000	0	Владивосток	свободен
b2c3d4e5-9999-2c3d-6e7f-8a9b0c1d2e3f	50000009	цистерна	66000	75	Омск	свободен
c3d4e5f6-0000-3d4e-7f8a-9b0c1d2e3f4a	50000010	рефрижератор	64000	105	Нижний Новгород	свободен
f5e4d3c2-2222-5b6c-9d0e-1f2a3b4c5d6e	50000002	полувагон	71000	85	Москва-Товарная	забронирован
c3d4e5f6-4444-7d8e-1f2a-3b4c5d6e7f8a	50000004	цистерна	68000	80	Самара	свободен
e5f6a1b2-6666-9f0a-3b4c-5d6e7f8a9b0c	50000006	крытый	68000	138	Казань	забронирован
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

\unrestrict 1qMxvUPzKo3N2OEKAc7KSVG6rsak4e9oHsLQLVB4CGgkHPyKoXxYkhtcB78WCdt

