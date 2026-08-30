package com.ligarecord.web.dto;

import com.ligarecord.domain.ResultadoJornada;

import java.util.UUID;

public record ResultadoDto(UUID id, UUID equipaId, String equipa, int pontuacao, int posicao) {

    public static ResultadoDto de(ResultadoJornada resultado) {
        return new ResultadoDto(
                resultado.getId(),
                resultado.getEquipa().getId(),
                resultado.getEquipa().getNome(),
                resultado.getPontuacao(),
                resultado.getPosicao()
        );
    }
}
