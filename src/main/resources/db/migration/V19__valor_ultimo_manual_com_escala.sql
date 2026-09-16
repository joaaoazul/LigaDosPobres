-- A V18 criou esta coluna como numeric sem escala nem limite, ao contrário de
-- todas as outras colunas de dinheiro do esquema (valor_inscricao,
-- valor_inicial, incremento, valor_maximo e bloco_divida.valor, todas
-- numeric(10,2) desde a V5; escala_valor.valor com check >= 0 desde a V12).
--
-- Duas consequências reais, as duas só visíveis muito depois do erro:
--  * um valor com mais casas (5,005) entrava tal e qual e só era arredondado
--    ao virar bloco de dívida — a regra e a dívida a discordarem uma da outra;
--  * um valor grande de mais (999999999) era aceite aqui e só rebentava ao
--    fechar a jornada, contra o numeric(10,2) do bloco, aparecendo ao gestor
--    como um 409 "foi alterado por outro pedido" que nunca mais o deixava
--    fechar a jornada.
--
-- O check de não-negativo existia só no RegraDividaService; passa a existir
-- também aqui, como nas outras colunas de dinheiro, para que nenhuma escrita
-- fora do serviço deixe um castigo negativo à espera de rebentar no fecho.
alter table regra_divida alter column valor_ultimo_manual type numeric(10,2);
alter table regra_divida add constraint valor_ultimo_manual_nao_negativo
    check (valor_ultimo_manual is null or valor_ultimo_manual >= 0);
