package com.ligarecord.web.dto;

import com.ligarecord.domain.BlocoDivida;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BlocoDividaDto(
        UUID id,
        int numeroBloco,
        String tipo,
        String nome,
        BigDecimal valor,
        String estado,
        Instant criadoEm,
        Instant resolvidoEm) {

    public static BlocoDividaDto de(BlocoDivida bloco) {
        return new BlocoDividaDto(
                bloco.getId(),
                bloco.getNumeroBloco(),
                bloco.getTipo().name(),
                bloco.getNome(),
                bloco.getValor(),
                bloco.getEstado().name(),
                bloco.getCriadoEm(),
                bloco.getResolvidoEm()
        );
    }
}
