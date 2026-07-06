-- English product description; the original "description" column stays Swedish.
alter table product
    add column description_en text;
