-- Contas de treinador: um treinador pode entrar e ver as equipas e dívidas
-- que tem em cada liga. Reutiliza a mesma tabela de contas do gestor — uma
-- pessoa pode gerir uma liga e treinar uma equipa sem precisar de dois
-- logins. A ligação é opcional: um treinador sem email, ou que não queira
-- criar conta, fica sem acesso próprio e o gestor continua a gerir a equipa
-- e as dívidas por ele.

alter table treinador
    add column conta_id uuid references gestor (id);

create table convite_treinador (
    id           uuid primary key,
    codigo       varchar(64)  not null unique,
    treinador_id uuid         not null references treinador (id),
    criado_por   uuid         not null references gestor (id),
    criado_em    timestamptz  not null,
    expira_em    timestamptz,
    usado_em     timestamptz,
    usado_por    uuid         references gestor (id),
    revogado_em  timestamptz,

    -- um convite usado tem sempre de dizer por quem: sem isto perdia-se o
    -- rasto de quem entrou com que convite
    constraint convite_treinador_uso_coerente check (
        (usado_em is null and usado_por is null) or
        (usado_em is not null and usado_por is not null)
    )
);

create index idx_convite_treinador_codigo on convite_treinador (codigo);
