-- 统一配送事件驱动的社区配送机器人服务 - 初始 schema

create table user_account (
    id           bigserial primary key,
    username     varchar(50)  not null unique,
    password     varchar(100) not null,
    display_name varchar(100) not null,
    role         varchar(20)  not null
);

create table community (
    id               bigserial primary key,
    name             varchar(64) not null,
    night_restricted boolean     not null default false,
    night_start      varchar(5)  not null default '22:00',
    night_end        varchar(5)  not null default '07:00'
);

create table building (
    id           bigserial primary key,
    community_id bigint      not null references community (id),
    name         varchar(32) not null,
    floors       int         not null default 1
);

create table robot (
    id           bigserial primary key,
    code         varchar(32) not null unique,
    community_id bigint      not null references community (id),
    battery      int         not null default 100,
    status       varchar(20) not null default 'IDLE',
    created_at   timestamp   not null default now()
);

create table cargo_compartment (
    id                 bigserial primary key,
    robot_id           bigint      not null references robot (id),
    compartment_no     varchar(10) not null,
    cold_chain_capable boolean     not null default false,
    status             varchar(20) not null default 'IDLE',
    unique (robot_id, compartment_no)
);

create table delivery_order (
    id                    bigserial primary key,
    order_no              varchar(40) not null unique,
    order_type            varchar(20) not null,
    community_id          bigint      not null references community (id),
    building_id           bigint      not null references building (id),
    unit_no               varchar(20),
    room_no               varchar(20),
    cold_chain            boolean     not null default false,
    estimated_delivery_at timestamp,
    user_name             varchar(64) not null,
    user_phone            varchar(32) not null,
    status                varchar(24) not null,
    robot_id              bigint references robot (id),
    compartment_no        varchar(10),
    relay_courier         varchar(64),
    assigned_at           timestamp,
    notified_at           timestamp,
    signed_at             timestamp,
    returned_at           timestamp,
    return_reason         varchar(255),
    cold_chain_deadline   timestamp,
    cold_chain_timed_out  boolean     not null default false,
    pickup_timed_out      boolean     not null default false,
    created_at            timestamp   not null,
    updated_at            timestamp   not null
);
create index idx_order_community on delivery_order (community_id, building_id, order_type);
create index idx_order_status on delivery_order (status);

create table access_request (
    id           bigserial primary key,
    delivery_id  bigint      not null references delivery_order (id),
    robot_id     bigint,
    gate_name    varchar(64) not null,
    status       varchar(20) not null,
    reason       varchar(255),
    requested_at timestamp   not null,
    decided_at   timestamp,
    decided_by   varchar(64)
);
create index idx_access_delivery on access_request (delivery_id);

create table elevator_request (
    id           bigserial primary key,
    delivery_id  bigint  not null references delivery_order (id),
    building_id  bigint  not null,
    from_floor   int     not null,
    to_floor     int     not null,
    status       varchar(20) not null,
    car_id       varchar(32),
    requested_at timestamp not null,
    resolved_at  timestamp
);
create index idx_elevator_delivery on elevator_request (delivery_id);

-- 统一配送事件流：门禁开通/路线调整/电梯请求/避障停留/签收/退回/客服介入等全部写入此表
create table delivery_event (
    id           varchar(64) primary key,
    delivery_id  bigint,
    community_id bigint,
    event_type   varchar(40) not null,
    actor        varchar(20) not null,
    reason       varchar(64),
    wait_seconds bigint,
    payload      text,
    occurred_at  timestamp   not null
);
create index idx_event_delivery on delivery_event (delivery_id, occurred_at);
create index idx_event_community on delivery_event (community_id, event_type);

create table complaint (
    id             bigserial primary key,
    delivery_id    bigint      not null references delivery_order (id),
    complaint_type varchar(30) not null,
    content        text,
    status         varchar(20) not null default 'OPEN',
    created_at     timestamp   not null,
    resolved_at    timestamp
);

create table cs_case (
    id             bigserial primary key,
    case_no        varchar(40) not null unique,
    delivery_id    bigint,
    complaint_id   bigint,
    case_type      varchar(30) not null,
    source         varchar(10) not null default 'AUTO',
    status         varchar(20) not null default 'OPEN',
    responsibility varchar(20) not null default 'NONE',
    note           text,
    handler        varchar(64),
    created_at     timestamp   not null,
    resolved_at    timestamp
);
create index idx_cs_case_delivery on cs_case (delivery_id);

create table notification (
    id          bigserial primary key,
    delivery_id bigint      not null references delivery_order (id),
    channel     varchar(20) not null,
    target      varchar(100) not null,
    content     text        not null,
    status      varchar(20) not null default 'SENT',
    sent_at     timestamp   not null
);
