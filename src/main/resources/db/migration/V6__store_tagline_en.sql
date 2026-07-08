-- English store tagline; the original "tagline" column stays Swedish.
alter table store_settings
    add column tagline_en varchar(200);

-- Existing installs carry the seeded Swedish tagline; give it its translation.
update store_settings
set tagline_en = 'Singles, sealed & accessories for collectors'
where tagline = 'Singles, sealed & tillbehör för samlare'
  and tagline_en is null;
