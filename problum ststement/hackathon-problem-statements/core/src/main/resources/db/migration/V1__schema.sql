create table plans (
    id                bigserial primary key,
    name              text not null unique,
    classes_per_month int not null,
    monthly_price     double precision not null,
    is_default        boolean not null default false
);

create table members (
    id            bigserial primary key,
    name          text not null,
    email         text not null unique,
    password_hash text not null,
    role          text not null default 'MEMBER',
    plan_id       bigint references plans (id),
    created_at    timestamptz not null default now()
);

create table sessions (
    token      text primary key,
    member_id  bigint not null references members (id),
    created_at timestamptz not null default now()
);

create table batches (
    id          bigserial primary key,
    name        text not null,
    instructor  text not null,
    description text not null default '',
    price       double precision not null
);

create table slots (
    id               bigserial primary key,
    batch_id         bigint not null references batches (id),
    starts_at        timestamptz not null,
    duration_minutes int not null,
    capacity         int not null,
    notes            text not null default ''
);

create index slots_starts_at on slots (starts_at);

create table bookings (
    id           bigserial primary key,
    slot_id      bigint not null references slots (id),
    member_id    bigint not null references members (id),
    status       text not null,
    amount       double precision not null,
    created_at   timestamptz not null default now(),
    cancelled_at timestamptz
);

create index bookings_slot on bookings (slot_id);
create index bookings_member on bookings (member_id);

create table payments (
    id         bigserial primary key,
    booking_id bigint not null references bookings (id),
    kind       text not null,
    amount     double precision not null,
    created_at timestamptz not null default now()
);

create table notifications (
    id         bigserial primary key,
    member_id  bigint not null references members (id),
    message    text not null,
    created_at timestamptz not null default now(),
    is_read    boolean not null default false
);

create table event_outbox (
    id           bigserial primary key,
    event_id     uuid not null unique,
    type         text not null,
    payload      text not null,
    created_at   timestamptz not null default now(),
    attempts     int not null default 0,
    delivered_at timestamptz
);
