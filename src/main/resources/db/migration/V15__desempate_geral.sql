-- A classificação geral desempatava sempre por nome. Agora o gestor pode
-- fixar a ordem entre equipas empatadas em pontos, e essa ordem passa a
-- decidir antes do nome — ver ClassificacaoService.
--
-- Sem valor (equipa nunca envolvida num desempate resolvido), fica null e o
-- nome continua a decidir, como sempre decidiu.
alter table equipa add column ordem_desempate integer;

-- Os pontos da equipa no momento em que ordem_desempate foi fixado. Sem
-- isto, um desempate resolvido a 10 pontos ficava a decidir também um
-- empate posterior e diferente, a 15 pontos, entre equipas que nunca
-- chegaram a ser comparadas — ordem_desempate só vale enquanto os pontos de
-- agora forem os mesmos de quando foi fixado.
alter table equipa add column ordem_desempate_pontos integer;
