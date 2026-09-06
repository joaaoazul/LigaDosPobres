package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.PoteDaLiga;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LigaDetalheDto(
        LigaDto liga,
        List<EquipaDto> equipas,
        List<JornadaDto> jornadas,
        List<ClassificacaoDto> classificacao,
        PoteDto pote) {

    /** A vista do treinador: sem estado de convites, que é assunto de quem gere a liga. */
    public static LigaDetalheDto de(Liga liga, List<ClassificacaoDto> classificacao, PoteDaLiga pote) {
        return de(liga, classificacao, pote, Map.of(), false);
    }

    /** A vista do gestor, com o estado da conta do treinador de cada equipa. */
    public static LigaDetalheDto deParaGestor(Liga liga,
                                              List<ClassificacaoDto> classificacao,
                                              PoteDaLiga pote,
                                              Map<UUID, ConviteTreinador> pendentesPorEquipa) {
        return de(liga, classificacao, pote, pendentesPorEquipa, true);
    }

    private static LigaDetalheDto de(Liga liga,
                                     List<ClassificacaoDto> classificacao,
                                     PoteDaLiga pote,
                                     Map<UUID, ConviteTreinador> pendentesPorEquipa,
                                     boolean paraGestor) {
        return new LigaDetalheDto(
                LigaDto.de(liga),
                liga.getEquipas().stream()
                        .map(equipa -> paraGestor
                                ? EquipaDto.deParaGestor(equipa, pendentesPorEquipa.get(equipa.getId()))
                                : EquipaDto.de(equipa))
                        .toList(),
                liga.getJornadas().stream().sorted(Jornada.ORDEM_CRONOLOGICA).map(JornadaDto::de).toList(),
                classificacao,
                PoteDto.de(pote)
        );
    }
}
