-- Dívidas por equipa: acumulam-se em blocos, um encargo por período. O valor
-- de cada bloco é, por agora, escrito à mão pelo gestor ao fechar o bloco — o
-- cálculo automático a partir da classificação (por escalões de equipas,
-- configurável por liga) fica para depois.

create table divida (
    id                    uuid primary key,
    equipa_id             uuid not null unique references equipa (id),
    estado                varchar(20) not null,
    -- protegido por "versao": duas transacções a acrescentar um bloco em
    -- simultâneo disputam esta linha, e uma delas falha em vez de as duas
    -- gravarem um bloco com o mesmo número.
    proximo_numero_bloco  int not null default 1,
    versao                bigint not null default 0
);

create table bloco_divida (
    id           uuid primary key,
    divida_id    uuid not null references divida (id),
    numero_bloco int not null,
    -- INSCRICAO (uma vez, ao entrar na liga) ou PERIODO (recorrente, por
    -- escalão de classificação) — ver regra_divida.
    tipo         varchar(20) not null,
    valor        numeric(10,2) not null,
    estado       varchar(20) not null,
    criado_em    timestamptz not null,
    resolvido_em timestamptz,

    -- um número de bloco não se repete dentro da mesma dívida
    constraint bloco_divida_numero_unico unique (divida_id, numero_bloco)
);

create index idx_bloco_divida_divida on bloco_divida (divida_id);

-- Como uma liga cobra as suas equipas: uma inscrição única na entrada, e um
-- valor por período de jornadas que sobe por escalão de classificação (quem
-- vai melhor paga menos). Sem esta regra, a liga não tem cobrança
-- automática nenhuma: o gestor fecha blocos e cobra a inscrição à mão.
create table regra_divida (
    id                   uuid primary key,
    liga_id              uuid not null unique references liga (id),
    valor_inscricao      numeric(10,2) not null,
    valor_inicial        numeric(10,2) not null,
    incremento           numeric(10,2) not null,
    equipas_por_escalao  int not null,
    valor_maximo         numeric(10,2) not null,
    jornadas_por_bloco   int not null
);
