package com.ligarecord.domain;

import java.math.BigDecimal;

/**
 * O dinheiro de uma liga, somado bloco a bloco: o que já foi lançado, o que o
 * gestor já deu por pago, e o que falta receber.
 *
 * <p>{@code pago} quer dizer "marcado como pago pelo gestor" — a aplicação não
 * mexe em dinheiro nenhum, só regista o que ele diz que recebeu.
 *
 * <p>O total é sempre a soma dos outros dois (só existem esses dois estados),
 * por isso é calculado a partir deles em vez de contado à parte: assim não há
 * forma de os três números se desencontrarem.
 */
public record PoteDaLiga(BigDecimal total, BigDecimal pago, BigDecimal porPagar) {

    public static PoteDaLiga de(BigDecimal pago, BigDecimal porPagar) {
        return new PoteDaLiga(pago.add(porPagar), pago, porPagar);
    }
}
