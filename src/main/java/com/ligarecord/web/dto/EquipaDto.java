package com.ligarecord.web.dto;

import com.ligarecord.domain.Equipa;

import java.util.UUID;

public record EquipaDto(UUID id, String nome, String treinador, String estado) {

    public static EquipaDto de(Equipa equipa) {
        return new EquipaDto(
                equipa.getId(),
                equipa.getNome(),
                equipa.getTreinador() == null ? null : equipa.getTreinador().getNome(),
                equipa.getEstado().name()
        );
    }
}
