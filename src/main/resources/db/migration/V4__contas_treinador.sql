-- Contas de treinador: um treinador pode entrar e ver as equipas e dívidas
-- que tem em cada liga. A conta é criada por convite do gestor dono da liga,
-- nunca por registo livre — só quem já tem uma equipa atribuída é convidável.

create table conta_treinador (
    id             uuid primary key,
    email          varchar(180) not null unique,
    password_hash  varchar(100) not null,
    nome           varchar(120) not null,
    criado_em      timestamptz  not null,
    ativo          boolean      not null default true,
    -- um treinador tem no máximo uma conta
    treinador_id   uuid         not null unique references treinador (id)
);

create table convite_treinador (
    id           uuid primary key,
    codigo       varchar(64)  not null unique,
    treinador_id uuid         not null references treinador (id),
    criado_por   uuid         not null references gestor (id),
    criado_em    timestamptz  not null,
    expira_em    timestamptz,
    usado_em     timestamptz,
    usado_por    uuid         references conta_treinador (id),
    revogado_em  timestamptz,

    -- um convite usado tem sempre de dizer por quem: sem isto perdia-se o
    -- rasto de quem entrou com que convite
    constraint convite_treinador_uso_coerente check (
        (usado_em is null and usado_por is null) or
        (usado_em is not null and usado_por is not null)
    )
);

create index idx_convite_treinador_codigo on convite_treinador (codigo);
