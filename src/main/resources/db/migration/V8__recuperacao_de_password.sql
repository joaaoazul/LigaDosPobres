-- Recuperação de password por email.
--
-- Guarda-se o resumo do código e não o código: quem lê esta tabela não fica com
-- as contas, porque o que vai no email nunca chega a ser escrito aqui. Nos
-- convites o código está em claro, e é uma diferença deliberada: um convite por
-- usar dá uma conta nova, um destes dá uma conta que já existe.

create table pedido_recuperacao (
    id          uuid primary key,
    codigo_hash varchar(64) not null unique,
    gestor_id   uuid        not null references gestor (id),
    criado_em   timestamptz not null,
    expira_em   timestamptz not null,
    usado_em    timestamptz,

    -- um pedido nasce sempre com validade à frente da criação; ao contrário,
    -- nascia expirado e ninguém dava por isso senão ao tentar usá-lo
    constraint pedido_recuperacao_validade check (expira_em > criado_em)
);

-- A procura é sempre pelo resumo, vinda de quem carregou no link do email.
create index idx_pedido_recuperacao_hash on pedido_recuperacao (codigo_hash);

-- Para travar o envio repetido de emails para a mesma conta.
create index idx_pedido_recuperacao_gestor on pedido_recuperacao (gestor_id, criado_em desc);

-- Mudar a password não expulsava ninguém: a sessão vive do lado do servidor e
-- não sabe nada da password. Numa recuperação isso é o pior caso, porque a
-- razão para recuperar costuma ser haver alguém na conta que não devia estar.
-- Fica nulo nas contas existentes: nunca mudaram a password, não há nada a
-- invalidar.
alter table gestor add column sessoes_validas_desde timestamptz;
