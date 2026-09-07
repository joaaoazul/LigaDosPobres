-- Nem todas as ligas cobram por uma fórmula.
--
-- A regra até aqui era "valor inicial + incremento por escalão, travado num
-- máximo": uma rampa de degraus todos iguais. Há ligas cuja tabela não é assim
-- — sobe 0,20€ por lugar até ao 7º e 0,10€ daí em diante, por exemplo — e não
-- há fórmula nenhuma que diga aquilo. Passa a poder ser uma tabela escrita à
-- mão, posição a posição, que é como essas ligas a têm no papel.
--
-- Tudo aqui nasce com o valor que as ligas existentes já têm hoje, para
-- nenhuma delas mudar de comportamento a meio de uma época.

alter table regra_divida add column escala varchar(10) not null default 'FORMULA';
alter table regra_divida add constraint regra_divida_escala_conhecida
    check (escala in ('FORMULA', 'TABELA'));

-- Há ligas em que as jornadas de treino não são cobradas. Nas que já existem
-- são — está no manual, "para efeitos de dinheiro e de classificação, as duas
-- contam igual" — e é esse o valor por omissão.
alter table regra_divida add column cobra_treino boolean not null default true;

-- E há ligas em que os pontos do treino também não contam para a
-- classificação: quando as oficiais começam, a tabela recomeça do zero. Vive na
-- liga e não na regra porque é do formato da prova, não da cobrança — uma liga
-- sem regra de dívida nenhuma continua a ter classificação.
alter table liga add column pontos_treino_contam boolean not null default true;

-- A tabela, quando é tabela: um valor por posição. Abaixo da última linha
-- repete-se o último valor, que é o que o "valor máximo" da fórmula já faz.
create table escala_valor (
    id       uuid primary key,
    regra_id uuid          not null references regra_divida (id) on delete cascade,
    posicao  int           not null check (posicao >= 1),
    valor    numeric(10,2) not null check (valor >= 0),

    -- duas linhas para a mesma posição deixavam o valor à sorte da ordem por
    -- que viessem da base de dados
    constraint escala_valor_posicao_unica unique (regra_id, posicao)
);

create index idx_escala_valor_regra on escala_valor (regra_id, posicao);
