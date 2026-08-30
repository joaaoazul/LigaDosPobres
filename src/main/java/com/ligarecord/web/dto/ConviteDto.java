package com.ligarecord.web.dto;

import com.ligarecord.domain.Convite;

import java.time.Instant;
import java.util.UUID;

public record ConviteDto(
        UUID id,
        String codigo,
        String nota,
        String estado,
        Instant criadoEm,
        Instant expiraEm,
        Instant usadoEm,
        String usadoPor) {

    public static ConviteDto de(Convite convite) {
        return new ConviteDto(
                convite.getId(),
                // O código só é útil enquanto o convite estiver por usar.
                convite.estaDisponivel() ? convite.getCodigo() : null,
                convite.getNota(),
                estado(convite),
                convite.getCriadoEm(),
                convite.getExpiraEm(),
                convite.getUsadoEm(),
                convite.getUsadoPor() == null ? null : convite.getUsadoPor().getEmail()
        );
    }

    private static String estado(Convite convite) {
        if (convite.estaUsado()) return "USADO";
        if (convite.estaRevogado()) return "REVOGADO";
        if (convite.estaExpirado()) return "EXPIRADO";
        return "DISPONIVEL";
    }
}
