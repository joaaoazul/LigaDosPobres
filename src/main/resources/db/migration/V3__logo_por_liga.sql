-- Logo por liga: cada gestor pode dar identidade visual à sua liga.
--
-- Os bytes ficam numa tabela à parte, e não numa coluna da própria liga, para
-- não serem arrastados em cada listagem de ligas (a listagem carrega a
-- entidade Liga inteira). A coluna logo_tipo, essa, fica na liga: é barata e
-- diz de imediato se existe logo e qual o seu tipo, sem tocar nos bytes.
alter table liga add column logo_tipo varchar(80);

create table liga_logo (
    liga_id uuid  primary key references liga (id) on delete cascade,
    dados   bytea not null
);
