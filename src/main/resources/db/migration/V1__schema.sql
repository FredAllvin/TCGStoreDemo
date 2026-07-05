-- TCG shop platform schema

create table store_settings (
    id                        bigint primary key,
    store_name                varchar(120) not null,
    tagline                   varchar(200),
    logo_path                 varchar(300),
    primary_color             varchar(20)  not null default '#1f2a44',
    accent_color              varchar(20)  not null default '#e8590c',
    currency                  varchar(3)   not null default 'SEK',
    default_locale            varchar(5)   not null default 'sv',
    eur_display_rate          numeric(12,6),
    contact_email             varchar(200),
    contact_phone             varchar(50),
    address_line              varchar(300),
    instagram_url             varchar(300),
    facebook_url              varchar(300),
    discord_url               varchar(300),
    free_ship_threshold_minor bigint
);

create table shipping_option (
    id          bigserial primary key,
    name        varchar(120) not null,
    price_minor bigint       not null check (price_minor >= 0),
    sort_order  int          not null default 0,
    active      boolean      not null default true
);

create table category (
    id         bigserial primary key,
    parent_id  bigint references category (id) on delete cascade,
    name       varchar(120) not null,
    slug       varchar(140) not null unique,
    sort_order int          not null default 0
);

create table product (
    id          bigserial primary key,
    category_id bigint       not null references category (id),
    name        varchar(200) not null,
    slug        varchar(220) not null unique,
    set_name    varchar(200),
    card_number varchar(50),
    rarity      varchar(80),
    language    varchar(40),
    description text,
    active      boolean      not null default true,
    featured    boolean      not null default false,
    created_at  timestamptz  not null default now()
);
create index idx_product_category on product (category_id);
create index idx_product_created on product (created_at desc);

create table product_image (
    id         bigserial primary key,
    product_id bigint       not null references product (id) on delete cascade,
    path       varchar(300) not null,
    alt        varchar(200),
    sort_order int          not null default 0
);
create index idx_product_image_product on product_image (product_id);

create table product_variant (
    id          bigserial primary key,
    product_id  bigint      not null references product (id) on delete cascade,
    kind        varchar(20) not null,
    condition   varchar(10),
    finish      varchar(20),
    grader      varchar(20),
    grade       varchar(20),
    cert_number varchar(60),
    sku         varchar(80),
    price_minor bigint      not null check (price_minor >= 0),
    stock_qty   int         not null default 0 check (stock_qty >= 0),
    image_path  varchar(300),
    active      boolean     not null default true
);
create index idx_variant_product on product_variant (product_id);

create table admin_user (
    id            bigserial primary key,
    username      varchar(80)  not null unique,
    password_hash varchar(100) not null
);

create table orders (
    id               bigserial primary key,
    order_number     varchar(20)  not null unique,
    access_token     varchar(40)  not null unique,
    status           varchar(20)  not null,
    customer_name    varchar(200) not null,
    email            varchar(200) not null,
    phone            varchar(50),
    address_line1    varchar(300) not null,
    address_line2    varchar(300),
    postal_code      varchar(20)  not null,
    city             varchar(120) not null,
    country          varchar(80)  not null,
    customer_note    varchar(1000),
    shipping_name    varchar(120) not null,
    shipping_minor   bigint       not null,
    subtotal_minor   bigint       not null,
    total_minor      bigint       not null,
    currency         varchar(3)   not null,
    payment_provider varchar(30),
    payment_ref      varchar(200),
    created_at       timestamptz  not null default now(),
    paid_at          timestamptz,
    shipped_at       timestamptz,
    cancelled_at     timestamptz
);
create index idx_orders_status on orders (status);
create index idx_orders_created on orders (created_at desc);

create table order_line (
    id               bigserial primary key,
    order_id         bigint       not null references orders (id) on delete cascade,
    variant_id       bigint references product_variant (id) on delete set null,
    product_name     varchar(200) not null,
    variant_label    varchar(200) not null,
    unit_price_minor bigint       not null,
    qty              int          not null check (qty > 0),
    line_total_minor bigint       not null
);
create index idx_order_line_order on order_line (order_id);

create table processed_webhook_event (
    id           bigserial primary key,
    event_id     varchar(120) not null unique,
    processed_at timestamptz  not null default now()
);

create sequence order_number_seq start with 1;

-- Singleton settings row; branding is edited from the admin panel.
insert into store_settings (id, store_name, tagline)
values (1, 'Kortbutiken', 'Singles, sealed & tillbehör för samlare');
