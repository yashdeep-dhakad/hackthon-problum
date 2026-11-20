-- V4: Fix issues reported by the front desk

-- T1.7: Fix truncated historical refund amounts in October to match gateway report
update payments set amount = 121.91 where id in (950001, 950007, 950013, 950019, 950025);
update payments set amount = 300.00 where id in (950002, 950008, 950014, 950020, 950026);
update payments set amount = 116.96 where id in (950005, 950011, 950017, 950023, 950029);
update payments set amount = 67.73 where id in (950006, 950012, 950018, 950024, 950030);

-- T1.5: Assign default plan to existing members whose plan_id is null
update members
set plan_id = (select id from plans where is_default = true limit 1)
where plan_id is null and role = 'MEMBER';

-- T1.3: Deduplicate confirmed bookings by cancelling duplicates and enforce unique constraint
with duplicates as (
    select id,
           row_number() over (partition by slot_id, member_id order by created_at asc, id asc) as rn
    from bookings
    where status = 'CONFIRMED'
)
update bookings
set status = 'CANCELLED', cancelled_at = now()
where id in (select id from duplicates where rn > 1);

create unique index if not exists unique_confirmed_booking
on bookings (slot_id, member_id)
where status = 'CONFIRMED';
