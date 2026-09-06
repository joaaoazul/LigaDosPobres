package com.ligarecord.web.dto;

import com.ligarecord.domain.PoteDaLiga;

import java.math.BigDecimal;

public record PoteDto(BigDecimal total, BigDecimal pago, BigDecimal porPagar) {

    public static PoteDto de(PoteDaLiga pote) {
        return new PoteDto(pote.total(), pote.pago(), pote.porPagar());
    }
}
