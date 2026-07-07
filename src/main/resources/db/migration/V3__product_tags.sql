-- Freeform search keywords ("pokemon, vintage"); matched by the shop search.
alter table product
    add column tags varchar(200);
