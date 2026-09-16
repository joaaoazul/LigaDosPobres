-- O formulário de contacto da página de entrada. Quem escreve não tem conta
-- nem sessão — é para isso que ele serve, para quem não consegue entrar — por
-- isso o endereço fica aberto a toda a gente e tem de se aguentar sozinho.
--
-- A mensagem é guardada, e não só enviada por email, por duas razões:
--  * se o Resend falhar (ou faltar a chave), a mensagem não se perde — ficou
--    aqui, e o email é a comodidade, não o único sítio onde ela existe;
--  * o travão ao spam precisa de contar pedidos por origem, e contá-los é
--    olhar para esta mesma tabela — como já se faz em tentativa_login_falhada.
--
-- Todos os campos têm limite: este é o segundo endereço da aplicação a que se
-- chega sem sessão nenhuma, e o primeiro (tentativa_login_falhada) já ensinou
-- que um varchar sem limite é um convite a encher a base de dados de graça.
create table mensagem_suporte (
    id         uuid primary key,
    nome       varchar(120)  not null,
    email      varchar(180)  not null,
    assunto    varchar(120)  not null,
    mensagem   varchar(4000) not null,
    -- 45 chega para IPv6. Fica nulo se o pedido chegar sem origem conhecida.
    ip         varchar(45),
    criado_em  timestamptz   not null
);

-- É por (ip, criado_em) que o travão conta os pedidos de cada origem.
create index idx_mensagem_suporte_ip_criado_em on mensagem_suporte (ip, criado_em);
