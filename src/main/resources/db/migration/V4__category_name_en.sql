-- English category name; the original "name" column stays Swedish.
alter table category
    add column name_en varchar(120);

-- Existing installs already carry the seeded accessories category; give it its translation.
update category
set name_en = 'Accessories'
where name = 'Tillbehör'
  and name_en is null;
