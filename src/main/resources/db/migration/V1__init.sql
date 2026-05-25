-- =============================================================================
-- Ford VIN Share, schema base (V1__init.sql)
-- Gerado a partir das entidades JPA (fonte de verdade) e validado contra o Neon.
-- PostgreSQL 15+. Inclui tipos enum, tabelas, constraints e indices.
-- =============================================================================


CREATE TYPE public.appointment_status AS ENUM (
    'SCHEDULED',
    'CHECKED_IN',
    'COMPLETED',
    'CANCELED',
    'NO_SHOW'
);



CREATE TYPE public.chat_message_role AS ENUM (
    'USER',
    'ASSISTANT',
    'SYSTEM'
);



CREATE TYPE public.customer_segment AS ENUM (
    'FIEL',
    'ECONOMICO',
    'ESQUECIDO',
    'ABANDONO'
);



CREATE TYPE public.device_platform AS ENUM (
    'IOS',
    'ANDROID'
);



CREATE TYPE public.lead_channel AS ENUM (
    'WHATSAPP',
    'EMAIL',
    'PHONE',
    'PUSH'
);



CREATE TYPE public.lead_status AS ENUM (
    'NEW',
    'IN_PROGRESS',
    'CONTACTED',
    'CONVERTED',
    'LOST'
);



CREATE TYPE public.loyalty_transaction_type AS ENUM (
    'EARN',
    'REDEEM',
    'EXPIRE',
    'ADJUSTMENT'
);



CREATE TYPE public.maintenance_alert_type AS ENUM (
    'OIL_CHANGE',
    'REVIEW',
    'BRAKE',
    'TIRE',
    'OTHER'
);



CREATE TYPE public.user_role AS ENUM (
    'CLIENT',
    'ANALYST',
    'ADMIN'
);



CREATE TYPE public.warranty_status AS ENUM (
    'ACTIVE',
    'EXPIRING_SOON',
    'EXPIRED'
);





CREATE TABLE public.analysts (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    dealership_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    full_name character varying(180) NOT NULL
);



CREATE TABLE public.appointments (
    created_at timestamp(6) with time zone NOT NULL,
    scheduled_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    customer_id uuid NOT NULL,
    dealership_id uuid NOT NULL,
    id uuid NOT NULL,
    vehicle_id uuid NOT NULL,
    service_type_id character varying(40) NOT NULL,
    notes character varying(500),
    status public.appointment_status NOT NULL,
    CONSTRAINT appointments_status_check CHECK ((status = ANY (ARRAY['SCHEDULED'::public.appointment_status, 'CHECKED_IN'::public.appointment_status, 'COMPLETED'::public.appointment_status, 'CANCELED'::public.appointment_status, 'NO_SHOW'::public.appointment_status])))
);



CREATE TABLE public.audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_id uuid,
    action character varying(80) NOT NULL,
    resource_type character varying(60),
    resource_id uuid,
    payload jsonb,
    ip character varying(45),
    user_agent character varying(255),
    correlation_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.chat_messages (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    session_id uuid NOT NULL,
    content text NOT NULL,
    role public.chat_message_role NOT NULL,
    suggested_actions jsonb,
    CONSTRAINT chat_messages_role_check CHECK ((role = ANY (ARRAY['USER'::public.chat_message_role, 'ASSISTANT'::public.chat_message_role, 'SYSTEM'::public.chat_message_role])))
);



CREATE TABLE public.chat_sessions (
    ended_at timestamp(6) with time zone,
    started_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);



CREATE TABLE public.customer_segments (
    risk_score numeric(5,2) NOT NULL,
    predicted_at timestamp(6) with time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    model_version character varying(40) NOT NULL,
    segment public.customer_segment NOT NULL,
    top_features jsonb,
    CONSTRAINT customer_segments_segment_check CHECK ((segment = ANY (ARRAY['FIEL'::public.customer_segment, 'ECONOMICO'::public.customer_segment, 'ESQUECIDO'::public.customer_segment, 'ABANDONO'::public.customer_segment])))
);



CREATE TABLE public.customers (
    birth_date date,
    created_at timestamp(6) with time zone NOT NULL,
    lgpd_consent_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    phone character varying(30),
    cpf_lookup_hash character varying(64) NOT NULL,
    full_name character varying(180) NOT NULL,
    cpf_encrypted text NOT NULL
);



CREATE TABLE public.dealership_services (
    active boolean NOT NULL,
    dealership_id uuid NOT NULL,
    service_type_id character varying(255) NOT NULL
);



CREATE TABLE public.dealerships (
    lat numeric(9,6) NOT NULL,
    lng numeric(9,6) NOT NULL,
    state character varying(2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    zip_code character varying(12),
    id uuid NOT NULL,
    phone character varying(30),
    city character varying(120) NOT NULL,
    name character varying(180) NOT NULL,
    address character varying(255) NOT NULL,
    opening_hours character varying(255)
);



CREATE TABLE public.device_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    platform public.device_platform NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    revoked_at timestamp with time zone
);






CREATE TABLE public.lead_actions (
    created_at timestamp(6) with time zone NOT NULL,
    analyst_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    template_id character varying(80),
    channel public.lead_channel NOT NULL,
    status public.lead_status NOT NULL,
    payload jsonb,
    CONSTRAINT lead_actions_channel_check CHECK ((channel = ANY (ARRAY['WHATSAPP'::public.lead_channel, 'EMAIL'::public.lead_channel, 'PHONE'::public.lead_channel, 'PUSH'::public.lead_channel]))),
    CONSTRAINT lead_actions_status_check CHECK ((status = ANY (ARRAY['NEW'::public.lead_status, 'IN_PROGRESS'::public.lead_status, 'CONTACTED'::public.lead_status, 'CONVERTED'::public.lead_status, 'LOST'::public.lead_status])))
);



CREATE TABLE public.loyalty_accounts (
    balance integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL
);



CREATE TABLE public.loyalty_transactions (
    points integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    source_reward_id uuid,
    source_service_id uuid,
    voucher_code character varying(40),
    type public.loyalty_transaction_type NOT NULL,
    CONSTRAINT loyalty_transactions_type_check CHECK ((type = ANY (ARRAY['EARN'::public.loyalty_transaction_type, 'REDEEM'::public.loyalty_transaction_type, 'EXPIRE'::public.loyalty_transaction_type, 'ADJUSTMENT'::public.loyalty_transaction_type])))
);



CREATE TABLE public.maintenance_alerts (
    due_date date,
    km_threshold integer,
    created_at timestamp(6) with time zone NOT NULL,
    dismissed_at timestamp(6) with time zone,
    id uuid NOT NULL,
    vehicle_id uuid NOT NULL,
    type public.maintenance_alert_type NOT NULL,
    CONSTRAINT maintenance_alerts_type_check CHECK ((type = ANY (ARRAY['OIL_CHANGE'::public.maintenance_alert_type, 'REVIEW'::public.maintenance_alert_type, 'BRAKE'::public.maintenance_alert_type, 'TIRE'::public.maintenance_alert_type, 'OTHER'::public.maintenance_alert_type])))
);



CREATE TABLE public.nps_responses (
    score smallint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    service_id uuid NOT NULL,
    comment character varying(1000),
    improvement_categories jsonb,
    liked_categories jsonb
);



CREATE TABLE public.parts_used (
    quantity integer NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    id uuid NOT NULL,
    service_id uuid NOT NULL,
    part_name character varying(180) NOT NULL
);



CREATE TABLE public.refresh_tokens (
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    revoked_at timestamp(6) with time zone,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL
);



CREATE TABLE public.rewards (
    active boolean NOT NULL,
    points_cost integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    name character varying(180) NOT NULL,
    description character varying(500)
);



CREATE TABLE public.service_types (
    free_with_warranty boolean NOT NULL,
    id character varying(40) NOT NULL,
    label character varying(120) NOT NULL
);



CREATE TABLE public.services (
    total_amount numeric(12,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    performed_at timestamp(6) with time zone NOT NULL,
    appointment_id uuid NOT NULL,
    dealership_id uuid NOT NULL,
    id uuid NOT NULL,
    vehicle_id uuid NOT NULL,
    service_type_id character varying(40) NOT NULL,
    summary character varying(500)
);



CREATE TABLE public.users (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    password_hash character varying(72) NOT NULL,
    email character varying(180) NOT NULL,
    role public.user_role NOT NULL,
    CONSTRAINT users_role_check CHECK ((role = ANY (ARRAY['CLIENT'::public.user_role, 'ANALYST'::public.user_role, 'ADMIN'::public.user_role])))
);



CREATE TABLE public.vehicles (
    current_km integer NOT NULL,
    year smallint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    plate character varying(10),
    customer_id uuid NOT NULL,
    id uuid NOT NULL,
    vin character varying(17) NOT NULL,
    model character varying(80) NOT NULL,
    version character varying(120)
);



CREATE TABLE public.warranties (
    end_date date NOT NULL,
    start_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    vehicle_id uuid NOT NULL,
    status public.warranty_status NOT NULL,
    CONSTRAINT warranties_status_check CHECK ((status = ANY (ARRAY['ACTIVE'::public.warranty_status, 'EXPIRING_SOON'::public.warranty_status, 'EXPIRED'::public.warranty_status])))
);



ALTER TABLE ONLY public.analysts
    ADD CONSTRAINT analysts_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.analysts
    ADD CONSTRAINT analysts_user_id_key UNIQUE (user_id);



ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT chat_sessions_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.customer_segments
    ADD CONSTRAINT customer_segments_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_cpf_lookup_hash_key UNIQUE (cpf_lookup_hash);



ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_user_id_key UNIQUE (user_id);



ALTER TABLE ONLY public.dealership_services
    ADD CONSTRAINT dealership_services_pkey PRIMARY KEY (dealership_id, service_type_id);



ALTER TABLE ONLY public.dealerships
    ADD CONSTRAINT dealerships_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_token_key UNIQUE (token);






ALTER TABLE ONLY public.lead_actions
    ADD CONSTRAINT lead_actions_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT loyalty_accounts_customer_id_key UNIQUE (customer_id);



ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT loyalty_accounts_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT loyalty_transactions_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.maintenance_alerts
    ADD CONSTRAINT maintenance_alerts_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.nps_responses
    ADD CONSTRAINT nps_responses_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.nps_responses
    ADD CONSTRAINT nps_responses_service_id_key UNIQUE (service_id);



ALTER TABLE ONLY public.parts_used
    ADD CONSTRAINT parts_used_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);



ALTER TABLE ONLY public.rewards
    ADD CONSTRAINT rewards_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.service_types
    ADD CONSTRAINT service_types_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_appointment_id_key UNIQUE (appointment_id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);



ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT vehicles_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT vehicles_vin_key UNIQUE (vin);



ALTER TABLE ONLY public.warranties
    ADD CONSTRAINT warranties_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.warranties
    ADD CONSTRAINT warranties_vehicle_id_key UNIQUE (vehicle_id);






CREATE INDEX idx_audit_log_action ON public.audit_log USING btree (action);



CREATE INDEX idx_audit_log_actor_created ON public.audit_log USING btree (actor_id, created_at DESC);



CREATE INDEX idx_audit_log_correlation ON public.audit_log USING btree (correlation_id);



CREATE INDEX idx_device_tokens_user ON public.device_tokens USING btree (user_id);



ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);



ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT fk1sbg0755loj68eucv8rxu4pao FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fk20oxyjhuhjpng5vinyb6fkk2f FOREIGN KEY (source_reward_id) REFERENCES public.rewards(id);



ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fk36trunt9fi7dhxxb4cqegyp5u FOREIGN KEY (service_type_id) REFERENCES public.service_types(id);



ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT fk3cpkdtwdxndrjhrx3gt9q5ux9 FOREIGN KEY (session_id) REFERENCES public.chat_sessions(id);



ALTER TABLE ONLY public.nps_responses
    ADD CONSTRAINT fk5jdlexbyprgs49e32ahtda72k FOREIGN KEY (service_id) REFERENCES public.services(id);



ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fk65epjrjy4kevtrnndyt9bkmxa FOREIGN KEY (source_service_id) REFERENCES public.services(id);



ALTER TABLE ONLY public.dealership_services
    ADD CONSTRAINT fk6tml1g5armmbi1mejqsbuuy5h FOREIGN KEY (dealership_id) REFERENCES public.dealerships(id);



ALTER TABLE ONLY public.chat_sessions
    ADD CONSTRAINT fk82ky97glaomlmhjqae1d0esmy FOREIGN KEY (user_id) REFERENCES public.users(id);



ALTER TABLE ONLY public.parts_used
    ADD CONSTRAINT fk8v330vql8igschqm21anvoxo1 FOREIGN KEY (service_id) REFERENCES public.services(id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT fk9htmf3b0pfrtfr35mduky2qrb FOREIGN KEY (service_type_id) REFERENCES public.service_types(id);



ALTER TABLE ONLY public.warranties
    ADD CONSTRAINT fka2ns0l8yopsnseuyb4ggdybev FOREIGN KEY (vehicle_id) REFERENCES public.vehicles(id);



ALTER TABLE ONLY public.maintenance_alerts
    ADD CONSTRAINT fkahfqrrfoiog1rtrxyl1ubfw87 FOREIGN KEY (vehicle_id) REFERENCES public.vehicles(id);



ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fkalpncq8pxtwld2wmgw4sxct70 FOREIGN KEY (vehicle_id) REFERENCES public.vehicles(id);



ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fke9pguirchy4kui3hqq8681vrr FOREIGN KEY (dealership_id) REFERENCES public.dealerships(id);



ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT fkfcst34t8dlye4nmvh70sb6d9j FOREIGN KEY (account_id) REFERENCES public.loyalty_accounts(id);



ALTER TABLE ONLY public.analysts
    ADD CONSTRAINT fkjq3sgpqudb7o6489ystdxm27y FOREIGN KEY (user_id) REFERENCES public.users(id);



ALTER TABLE ONLY public.vehicles
    ADD CONSTRAINT fkjrosretvs9ih5ybhpsd5qskc3 FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT fklph60ok9q3oibqawro4eawrj FOREIGN KEY (vehicle_id) REFERENCES public.vehicles(id);



ALTER TABLE ONLY public.dealership_services
    ADD CONSTRAINT fkm2elwqhjmb1w5mx40unqvvkgc FOREIGN KEY (service_type_id) REFERENCES public.service_types(id);



ALTER TABLE ONLY public.analysts
    ADD CONSTRAINT fkmyw2g46i5pbfscano950g4ax6 FOREIGN KEY (dealership_id) REFERENCES public.dealerships(id);



ALTER TABLE ONLY public.nps_responses
    ADD CONSTRAINT fknsxxcte7365ahoic0e4vt5afr FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.lead_actions
    ADD CONSTRAINT fkpmkgijih7ts2kbaure6b8q00n FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.customer_segments
    ADD CONSTRAINT fkq5xa229dp34dftbrag0xi8k71 FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fkrh1g1a20omjmn6kurd35o3eit FOREIGN KEY (user_id) REFERENCES public.users(id);



ALTER TABLE ONLY public.lead_actions
    ADD CONSTRAINT fkrl2gnk255sxnrt8hf3rwsv9ne FOREIGN KEY (analyst_id) REFERENCES public.analysts(id);



ALTER TABLE ONLY public.appointments
    ADD CONSTRAINT fkrlbb09f329sfsmftrh7y0yxtk FOREIGN KEY (customer_id) REFERENCES public.customers(id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT fkshrt5nefnplp9g1arsj4yumk3 FOREIGN KEY (dealership_id) REFERENCES public.dealerships(id);



ALTER TABLE ONLY public.services
    ADD CONSTRAINT fksyjagx63q04aaa6wu7m0cttl4 FOREIGN KEY (appointment_id) REFERENCES public.appointments(id);

-- =============================================================================
-- Seed minimo de catalogo
-- =============================================================================
INSERT INTO public.service_types (id, label, free_with_warranty) VALUES
  ('REVIEW',     'Revisao',       true),
  ('OIL_CHANGE', 'Troca de oleo', false),
  ('WARRANTY',   'Garantia',      true),
  ('REPAIR',     'Reparo',        false);
