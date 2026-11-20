-- Module migrations use their own version range: reminders owns 100.x.
create table reminders_settings (
    member_id      bigint primary key,
    enabled        boolean not null default false,
    minutes_before int not null default 120
);

create table reminders_confirmations (
    event_id    uuid primary key,
    received_at timestamptz not null default now()
);

create table reminders_sent (
    booking_id bigint primary key,
    sent_at    timestamptz not null default now()
);
