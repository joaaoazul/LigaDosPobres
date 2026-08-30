-- Esquema inicial da Liga dos Pobres.
-- As restrições aqui declaradas duplicam de propósito regras que também existem
-- no código: o que o Java verifica, um bug pode contornar; o que a base de dados
-- impõe, não.

create table gestor (
    id             uuid primary key,
    email          varchar(180) not null unique,
    password_hash  varchar(100) not null,
    nome           varchar(120) not null,
    criado_em      timestamptz  not null
);

create table treinador (
    id    uuid primary key,
    nome  varchar(120) not null
);

create table liga (
    id           uuid primary key,
    nome         varchar(120) not null,
    max_equipas  int          not null check (max_equipas between 1 and 45),
    estado       varchar(20)  not null,
    gestor_id    uuid         not null references gestor (id)
);

-- Todas as listagens filtram por gestor; sem este índice cada uma faz varrimento
-- completo da tabela.
create index idx_liga_gestor on liga (gestor_id);

create table equipa (
    id            uuid primary key,
    nome          varchar(120) not null,
    estado        varchar(20)  not null,
    liga_id       uuid         references liga (id) on delete cascade,
    treinador_id  uuid         not null references treinador (id)
);

create index idx_equipa_liga on equipa (liga_id);

create table jornada (
    id           uuid primary key,
    num_jornada  int         not null,
    estado       varchar(20) not null,
    tipo         varchar(20) not null,
    liga_id      uuid        not null references liga (id) on delete cascade,
    -- não pode haver duas jornadas de treino nº 3 na mesma liga
    unique (liga_id, tipo, num_jornada)
);

create index idx_jornada_liga on jornada (liga_id);

create table resultado_jornada (
    id                uuid    primary key,
    jornada_id        uuid    not null references jornada (id) on delete cascade,
    equipa_id         uuid    not null references equipa (id),
    pontuacao         int     not null check (pontuacao >= 0),
    posicao           int     not null default 0,
    desempate_manual  boolean not null default false,
    -- uma equipa tem no máximo um resultado por jornada
    unique (jornada_id, equipa_id)
);

create index idx_resultado_jornada on resultado_jornada (jornada_id);
