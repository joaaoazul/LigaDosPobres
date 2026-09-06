alter table jornada add column incluida_em_bloco boolean not null default false;

-- jornadas já fechadas antes desta migração já foram tidas em conta pelo
-- mecanismo antigo (sem esta marca); tratá-las como já incluídas evita que o
-- próximo fecho de bloco as some outra vez.
update jornada set incluida_em_bloco = true where estado = 'FECHADA';
