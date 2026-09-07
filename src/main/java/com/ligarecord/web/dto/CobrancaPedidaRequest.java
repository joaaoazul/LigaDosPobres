package com.ligarecord.web.dto;

/**
 * Uma cobrança tal como o gestor a escreve: o nome que vai aparecer na dívida,
 * a jornada <b>oficial</b> em que cai, e a sua tabela de valores em texto —
 * uma linha por posição, como a da regra.
 */
public record CobrancaPedidaRequest(String nome, Integer jornadaOficial, String tabela) {
}
