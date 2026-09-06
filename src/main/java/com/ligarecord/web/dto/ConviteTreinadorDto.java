package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;

import java.time.Instant;
import java.util.UUID;

/**
 * O convite como o gestor o vê. O {@code link} é o que ele entrega ao
 * treinador — um endereço, e não um código de 32 caracteres para escrever à
 * mão — e, tal como o código, só existe enquanto o convite estiver por usar.
 */
public record ConviteTreinadorDto(
        UUID id,
        String codigo,
        String link,
        String treinadorNome,
        String equipaNome,
        String estado,
        Instant criadoEm,
        Instant expiraEm,
        Instant usadoEm) {

    public static ConviteTreinadorDto de(ConviteTreinador convite, String link) {
        boolean disponivel = convite.estaDisponivel();
        return new ConviteTreinadorDto(
                convite.getId(),
                // O código só é útil enquanto o convite estiver por usar.
                disponivel ? convite.getCodigo() : null,
                disponivel ? link : null,
                convite.getTreinador().getNome(),
                convite.getEquipa().getNome(),
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
