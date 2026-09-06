-- O convite de treinador passa a ser um convite a um lugar: "treinas a equipa X
-- da liga Y", e não "és o treinador João".
--
-- A aplicação já se comportava assim — cada equipa criada gera a sua linha de
-- treinador, e a identidade da pessoa é a conta, não o treinador — mas nada
-- disso estava escrito no esquema: duas equipas podiam partilhar a mesma linha
-- de treinador, e o convite não sabia dizer de que equipa falava.

-- Falha em vez de escolher por nós. Se houver treinadores partilhados, o
-- convite era ambíguo (ligava duas equipas de uma vez) e a correcção depende de
-- quem conhece os dados: separar as linhas ou juntar as equipas.
do $$
declare
    partilhados int;
begin
    select count(*) into partilhados from (
        select treinador_id from equipa group by treinador_id having count(*) > 1
    ) repetidos;

    if partilhados > 0 then
        raise exception 'Há % treinador(es) partilhado(s) por mais do que uma equipa. '
                        'Separa as linhas em treinador antes de aplicar esta migração: '
                        'select treinador_id, count(*) from equipa group by treinador_id having count(*) > 1;',
                        partilhados;
    end if;
end $$;

-- Uma linha de treinador é o lugar de treinador de uma equipa. Passa a ser
-- facto da base de dados e não convenção do código.
alter table equipa add constraint equipa_treinador_unico unique (treinador_id);

alter table convite_treinador add column equipa_id uuid references equipa (id);

-- Com a unicidade acima, cada convite tem exactamente uma equipa candidata.
update convite_treinador c
   set equipa_id = e.id
  from equipa e
 where e.treinador_id = c.treinador_id;

-- Um convite sem equipa depois disto é um convite a um treinador que não treina
-- nada: não pode existir (só se emite a partir de uma equipa) e mais vale
-- rebentar aqui do que carregar a coluna nula para sempre. A verificação
-- explícita existe para a mensagem dizer o que se passa, em vez de sair a
-- violação de not null em cru.
do $$
declare
    orfaos int;
begin
    select count(*) into orfaos from convite_treinador where equipa_id is null;

    if orfaos > 0 then
        raise exception 'Há % convite(s) de treinador sem equipa correspondente. '
                        'Apaga-os ou liga-os à equipa certa antes de aplicar esta migração: '
                        'select id, treinador_id from convite_treinador where equipa_id is null;',
                        orfaos;
    end if;
end $$;

alter table convite_treinador alter column equipa_id set not null;

-- Emitir um convite passa a procurar primeiro o pendente que já exista para
-- aquele lugar, em vez de criar um segundo.
create index idx_convite_treinador_pendente on convite_treinador (treinador_id)
    where usado_em is null and revogado_em is null;

create index idx_convite_treinador_equipa on convite_treinador (equipa_id);
