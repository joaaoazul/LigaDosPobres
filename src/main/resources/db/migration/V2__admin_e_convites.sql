-- Administração: papéis de gestor e convites individuais.
-- Substitui o código de registo único que vinha de variável de ambiente.

alter table gestor
    add column papel varchar(20)  not null default 'GESTOR',
    add column ativo boolean      not null default true;

create table convite (
    id          uuid primary key,
    codigo      varchar(64)  not null unique,
    nota        varchar(160),
    criado_por  uuid         not null references gestor (id),
    criado_em   timestamptz  not null,
    expira_em   timestamptz,
    usado_em    timestamptz,
    usado_por   uuid         references gestor (id),
    revogado_em timestamptz,

    -- um convite usado tem sempre de dizer por quem: sem isto perdia-se o
    -- rasto de quem entrou com que convite
    constraint convite_uso_coerente check (
        (usado_em is null and usado_por is null) or
        (usado_em is not null and usado_por is not null)
    )
);

create index idx_convite_codigo on convite (codigo);
