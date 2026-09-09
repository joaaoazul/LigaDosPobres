-- Nada travava tentativas repetidas de adivinhar uma password, além da
-- lentidão própria do BCrypt. Cada login falhado fica registado por email (não
-- por conta: um email sem conta nenhuma tem de contar tanto como um que
-- existe, senão o bloqueio dizia sozinho quais os emails com conta) e
-- LimiteDeLoginService trava quem passar do limite numa janela de tempo.
-- Mesmo limite do email de gestor e de treinador (RegrasDeConta.MAXIMO_EMAIL):
-- sem ele, um pedido de login com um email gigante engordava esta tabela para
-- sempre — é a única das tabelas de credenciais que nunca é limpa, e ninguém
-- precisa de sessão nenhuma para lhe bater.
create table tentativa_login_falhada (
    id uuid primary key,
    email varchar(180) not null,
    criado_em timestamptz not null
);

-- É por (email, criado_em) que cada tentativa é contada.
create index idx_tentativa_login_email_criado_em on tentativa_login_falhada (email, criado_em);
