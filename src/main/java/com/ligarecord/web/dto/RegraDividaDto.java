package com.ligarecord.web.dto;

import com.ligarecord.domain.EscalaValor;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.enums.EscalaDivida;
import com.ligarecord.service.EscalaColada;

import java.math.BigDecimal;
import java.util.UUID;

public record RegraDividaDto(
        UUID id,
        BigDecimal valorInscricao,
        BigDecimal valorInicial,
        BigDecimal incremento,
        int equipasPorEscalao,
        BigDecimal valorMaximo,
        int jornadasPorBloco,
        String escala,
        String tabela,
        boolean cobraTreino) {

    public static RegraDividaDto de(RegraDivida regra) {
        return new RegraDividaDto(
                regra.getId(),
                regra.getValorInscricao(),
                regra.getValorInicial(),
                regra.getIncremento(),
                regra.getEquipasPorEscalao(),
                regra.getValorMaximo(),
                regra.getJornadasPorBloco(),
                regra.getEscala().name(),
                // De volta a texto, na mesma forma em que foi colada: é assim
                // que o gestor a relê e corrige.
                regra.getEscala() == EscalaDivida.TABELA
                        ? EscalaColada.escrever(regra.getTabela().stream().map(EscalaValor::getValor).toList())
                        : null,
                regra.isCobraTreino()
        );
    }
}
