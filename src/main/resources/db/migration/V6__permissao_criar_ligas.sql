-- Só uma conta com pode_criar_ligas pode criar (e portanto gerir) as suas
-- próprias ligas. As contas criadas pelo convite normal de gestor já nascem
-- assim; as criadas por convite de treinador nascem sem — só um
-- administrador as promove a partir daqui. Sem isto, quem só foi convidado
-- para treinar uma equipa podia começar ligas próprias sem pagar nada.
alter table gestor
    add column pode_criar_ligas boolean not null default true;
