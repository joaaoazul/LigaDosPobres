-- Fecha à chave o que a V9 só prometia em código: um convite vivo por lugar,
-- e nenhum sem prazo.
--
-- Os dois estados que faltavam arrumar existem em bases anteriores à V9, e não
-- há forma de a aplicação os voltar a produzir:
--   * convites sem prazo, que a interface antiga criava sempre (diasValidade
--     null queria dizer "para sempre");
--   * mais do que um convite por usar para o mesmo treinador, de quando emitir
--     criava um novo a cada carregar do botão.

-- Um convite sem prazo é uma credencial que anda numa conversa para sempre.
-- Trinta dias a partir daqui, que é a validade por omissão: quem ainda não o
-- usou tem tempo de sobra, e quem perder o prazo pede outro, que é um clique.
-- Só os que ainda estão vivos — nos gastos e revogados a coluna já não decide
-- nada, e reescrevê-la era mexer no histórico.
update convite_treinador
   set expira_em = now() + interval '30 days'
 where expira_em is null
   and usado_em is null
   and revogado_em is null;

-- Dos convites por usar do mesmo treinador fica o mais recente. Os outros são
-- revogados, que é o que a aplicação passou a fazer sozinha quando o lugar
-- ganha conta: nunca se apaga um convite, fica o rasto de quem convidou quem.
update convite_treinador c
   set revogado_em = now()
 where c.usado_em is null
   and c.revogado_em is null
   and exists (
       select 1
         from convite_treinador mais_novo
        where mais_novo.treinador_id = c.treinador_id
          and mais_novo.usado_em is null
          and mais_novo.revogado_em is null
          -- o id desempata: duas linhas criadas no mesmo instante têm de dar
          -- sempre a mesma vencedora, ou a migração não é determinista
          and (mais_novo.criado_em, mais_novo.id) > (c.criado_em, c.id)
   );

-- E agora a regra passa a ser da base de dados. Emitir procura primeiro o
-- convite por usar que já exista, mas isso é ler-e-depois-escrever: dois
-- pedidos ao mesmo tempo passavam os dois pela leitura e criavam dois códigos
-- válidos para a mesma equipa. Com o índice, o segundo falha — que é o que se
-- quer, porque a alternativa é uma credencial a mais de que ninguém sabe.
drop index idx_convite_treinador_pendente;

create unique index idx_convite_treinador_pendente on convite_treinador (treinador_id)
    where usado_em is null and revogado_em is null;
