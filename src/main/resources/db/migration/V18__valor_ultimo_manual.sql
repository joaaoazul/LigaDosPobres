-- Escala por fórmula trava tudo a partir de um certo escalão no mesmo
-- valor_maximo, mas há gestores que querem o último classificado de cada
-- jornada a pagar um valor próprio (normalmente maior, como castigo) em vez
-- de partilhar o máximo com todos os outros do último escalão. Fica de fora
-- da fórmula em si: sem valor definido, nada muda.
alter table regra_divida add column valor_ultimo_manual numeric;
