package com.ligarecord.web.dto;

import java.math.BigDecimal;

public record DefinirRegraDividaRequest(
        BigDecimal valorInscricao,
        BigDecimal valorInicial,
        BigDecimal incremento,
        Integer equipasPorEscalao,
        BigDecimal valorMaximo,
        Integer jornadasPorBloco) {
}
