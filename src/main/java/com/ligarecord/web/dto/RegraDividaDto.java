package com.ligarecord.web.dto;

import com.ligarecord.domain.RegraDivida;

import java.math.BigDecimal;
import java.util.UUID;

public record RegraDividaDto(
        UUID id,
        BigDecimal valorInscricao,
        BigDecimal valorInicial,
        BigDecimal incremento,
        int equipasPorEscalao,
        BigDecimal valorMaximo,
        int jornadasPorBloco) {

    public static RegraDividaDto de(RegraDivida regra) {
        return new RegraDividaDto(
                regra.getId(),
                regra.getValorInscricao(),
                regra.getValorInicial(),
                regra.getIncremento(),
                regra.getEquipasPorEscalao(),
                regra.getValorMaximo(),
                regra.getJornadasPorBloco()
        );
    }
}
