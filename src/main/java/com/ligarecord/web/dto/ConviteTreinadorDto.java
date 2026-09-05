package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;

import java.time.Instant;
import java.util.UUID;

public record ConviteTreinadorDto(
        UUID id,
        String codigo,
        String treinadorNome,
        String estado,
        Instant criadoEm,
        Instant expiraEm,
        Instant usadoEm) {

    public static ConviteTreinadorDto de(ConviteTreinador convite) {
        return new ConviteTreinadorDto(
                convite.getId(),
                // O código só é útil enquanto o convite estiver por usar.
                convite.estaDisponivel() ? convite.getCodigo() : null,
                convite.getTreinador().getNome(),
                estado(convite),
                convite.getCriadoEm(),
                convite.getExpiraEm(),
                convite.getUsadoEm()
        );
    }

    private static String estado(ConviteTreinador convite) {
        if (convite.estaUsado()) return "USADO";
        if (convite.estaRevogado()) return "REVOGADO";
        if (convite.estaExpirado()) return "EXPIRADO";
        return "DISPONIVEL";
    }
}
