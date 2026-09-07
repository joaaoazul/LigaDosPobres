package com.ligarecord.web.dto;

import java.math.BigDecimal;

/**
 * {@code escala} diz de onde sai o valor de cada posição: {@code "FORMULA"}
 * (os quatro campos do meio) ou {@code "TABELA"} — e aí a {@code tabela} vem
 * como texto, uma linha por posição, tal como o gestor a tem escrita.
 *
 * <p>{@code escala} e {@code cobraTreino} a null valem o que a aplicação sempre
 * fez: fórmula, e treino cobrado como qualquer outra jornada.
 */
public record DefinirRegraDividaRequest(
        BigDecimal valorInscricao,
        BigDecimal valorInicial,
        BigDecimal incremento,
        Integer equipasPorEscalao,
        BigDecimal valorMaximo,
        Integer jornadasPorBloco,
        String escala,
        String tabela,
        Boolean cobraTreino) {
}
