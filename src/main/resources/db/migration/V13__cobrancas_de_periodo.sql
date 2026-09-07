-- Cobranças presas a uma jornada, pela classificação geral.
--
-- Há ligas com uma segunda cobrança por cima da de cada jornada: a meio da
-- época e no fim, cada equipa paga conforme o lugar em que está na
-- classificação geral. É outra coisa da cobrança por bloco — não é a posição
-- daquela jornada, é a da tabela acumulada, e acontece uma vez, numa jornada
-- combinada de antemão.

create table cobranca_periodo (
    id              uuid        primary key,
    regra_id        uuid        not null references regra_divida (id) on delete cascade,
    nome            varchar(40) not null,
    -- o número da jornada OFICIAL em que cai. Oficial e não a contagem corrida
    -- desde o início: as de treino podem ser mais ou menos do que se espera, e
    -- uma cobrança que escorrega uma jornada é dinheiro cobrado no sítio errado
    jornada_oficial int         not null check (jornada_oficial >= 1),
    cobrada_em      timestamptz,

    -- o nome é o que aparece na dívida de cada equipa ("Inverno"), por isso
    -- dois iguais na mesma liga não se distinguiriam um do outro
    constraint cobranca_periodo_nome_unico unique (regra_id, nome)
);

create index idx_cobranca_periodo_regra on cobranca_periodo (regra_id);

-- A tabela desta cobrança, que é sua e não a da jornada: pode ir de 0€ a 10€
-- enquanto a semanal vai de 0€ a 2,50€.
create table cobranca_valor (
    id          uuid          primary key,
    cobranca_id uuid          not null references cobranca_periodo (id) on delete cascade,
    posicao     int           not null check (posicao >= 1),
    valor       numeric(10,2) not null check (valor >= 0),

    constraint cobranca_valor_posicao_unica unique (cobranca_id, posicao)
);

create index idx_cobranca_valor_cobranca on cobranca_valor (cobranca_id, posicao);

-- O bloco passa a poder dizer de onde veio. Nulo nos que já existem e em todos
-- os de inscrição e de período: só as cobranças com nome próprio o preenchem,
-- para na lista de dívidas se ler "Inverno" e não "Bloco 12".
alter table bloco_divida add column nome varchar(40);
