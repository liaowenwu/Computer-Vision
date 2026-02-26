create table if not exists product_type (
    id bigserial primary key,
    type_code varchar(64) unique not null,
    type_name varchar(128) not null,
    parent_id bigint,
    sort_no integer default 0,
    status smallint default 1,
    created_at timestamp default now(),
    updated_at timestamp default now()
);

create table if not exists product (
    id bigserial primary key,
    sku varchar(64) unique not null,
    product_name varchar(255) not null,
    brand varchar(128),
    type_id bigint,
    status smallint default 1,
    created_at timestamp default now(),
    updated_at timestamp default now()
);

create table if not exists supplier (
    id bigserial primary key,
    supplier_name varchar(255) not null,
    company_name varchar(255),
    region varchar(128),
    status smallint default 1,
    created_at timestamp default now(),
    updated_at timestamp default now()
);

create table if not exists sync_task (
    id bigserial primary key,
    task_no varchar(64) unique not null,
    task_type varchar(16) not null,
    trigger_by varchar(64),
    status varchar(16) not null,
    total_count integer default 0,
    success_count integer default 0,
    fail_count integer default 0,
    error_message text,
    started_at timestamp,
    finished_at timestamp,
    created_at timestamp default now()
);

create table if not exists task_log (
    id bigserial primary key,
    task_id bigint not null,
    task_no varchar(64) not null,
    level varchar(16) not null,
    message text not null,
    sku varchar(64),
    created_at timestamp default now()
);

create table if not exists price_snapshot (
    id bigserial primary key,
    sku varchar(64) not null,
    product_name varchar(255) not null,
    brand varchar(128),
    region varchar(128),
    company_name varchar(255),
    supplier_name varchar(255) not null,
    stock integer,
    price numeric(12, 2) not null,
    snapshot_time timestamp default now(),
    snapshot_date date not null,
    task_id bigint,
    created_at timestamp default now(),
    constraint uk_snapshot unique (sku, supplier_name, snapshot_date)
);

create index if not exists idx_snapshot_sku_date on price_snapshot(sku, snapshot_date desc);
create index if not exists idx_task_log_task_no on task_log(task_no, created_at);
