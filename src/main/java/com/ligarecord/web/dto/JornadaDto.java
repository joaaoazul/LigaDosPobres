package com.ligarecord.web.dto;

import com.ligarecord.domain.Jornada;

import java.util.List;
import java.util.UUID;

public record JornadaDto(
        UUID id,
        int numero,
        String estado,
        String tipo,
        boolean treino,
        List<ResultadoDto> resultados) {

    public static JornadaDto de(Jornada jornada) {
        return new JornadaDto(
                jornada.getId(),
                jornada.getNumJornada(),
                jornada.getEstadoJ().name(),
                jornada.getTipoJornada() == null ? null : jornada.getTipoJornada().name(),
                jornada.iseTreino(),
                jornada.getResultadoJ().stream().map(ResultadoDto::de).toList()
        );
    }
}
