package com.ligarecord.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code escala} diz de onde sai o valor de cada posição: {@code "FORMULA"}
 * (os quatro campos do meio) ou {@code "TABELA"} — e aí a {@code tabela} vem
 * como texto, uma linha por posição, tal como o gestor a tem escrita.
 *
 * <p>{@code escala} e {@code cobraTreino} a null valem o que a aplicação sempre
 * fez: fórmula, e treino cobrado como qualquer outra jornada. {@code cobrancas}
 * a null deixa as que existem como estão; uma lista vazia apaga-as.
 *
 * <p>{@code valorUltimoManual} a null <b>apaga</b> o valor que lá estivesse —
 * ao contrário de {@code cobrancas}, e de propósito: é assim que o gestor o
 * tira, deixando o campo em branco, e não há outro valor que queira dizer
 * "sem castigo para o último". A consequência é que um cliente que não envie
 * o campo o apaga sem dar por isso; quem faz um PUT a esta regra tem de
 * mandar a regra inteira.
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
        Boolean cobraTreino,
        List<CobrancaPedidaRequest> cobrancas,
        BigDecimal valorUltimoManual) {
}
