-- O email do treinador, para o convite deixar de ser entregue à mão.
--
-- É contacto do lugar, dado pelo gestor, e não uma credencial: quem aceita o
-- convite escolhe o email da conta no registo, e pode ser outro. Fica opcional
-- de propósito — um treinador sem email continua a ser um treinador, e o gestor
-- continua a poder entregar-lhe o link como quiser.
alter table treinador add column email varchar(180);

-- Rasto do que foi enviado e para onde. Sem isto não há como travar o envio
-- repetido, nem como responder à pergunta "isto chegou a sair?".
alter table convite_treinador add column enviado_em   timestamptz;
alter table convite_treinador add column enviado_para varchar(180);
alter table convite_treinador add column envios       int not null default 0;

-- Um convite com envios contados tem sempre data do último, e ao contrário
-- também: sem isto, um dos dois campos podia ficar para trás numa alteração
-- futura e ninguém dava por isso senão ao ler o rasto.
alter table convite_treinador add constraint convite_treinador_envio_coerente check (
    (envios = 0 and enviado_em is null and enviado_para is null) or
    (envios > 0 and enviado_em is not null and enviado_para is not null)
);
