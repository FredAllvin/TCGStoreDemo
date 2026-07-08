-- English shipping option name; the original "name" column stays Swedish.
alter table shipping_option
    add column name_en varchar(120);

-- Existing installs carry the seeded Swedish options; give them their translations.
update shipping_option set name_en = 'Letter (no tracking)'      where name = 'Postbrev (ej spårbart)' and name_en is null;
update shipping_option set name_en = 'PostNord tracked parcel'   where name = 'PostNord spårbart paket' and name_en is null;
update shipping_option set name_en = 'Pick up in store'          where name = 'Hämta i butik' and name_en is null;

-- Orders snapshot the shipping name at purchase time; snapshot both languages.
alter table orders
    add column shipping_name_en varchar(120);
