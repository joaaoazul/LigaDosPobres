package com.ligarecord.web.dto;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;

import java.util.List;

public record LigaDetalheDto(
        LigaDto liga,
        List<EquipaDto> equipas,
        List<JornadaDto> jornadas,
        List<ClassificacaoDto> classificacao) {

    public static LigaDetalheDto de(Liga liga, List<ClassificacaoDto> classificacao) {
        return new LigaDetalheDto(
                LigaDto.de(liga),
                liga.getEquipas().stream().map(EquipaDto::de).toList(),
                liga.getJornadas().stream().sorted(Jornada.ORDEM_CRONOLOGICA).map(JornadaDto::de).toList(),
                classificacao
        );
    }
}
