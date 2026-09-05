package com.ligarecord.web.dto;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DividaDto(
        UUID id,
        UUID equipaId,
        String equipaNome,
        UUID ligaId,
        String ligaNome,
        String estado,
        BigDecimal totalPendente,
        List<BlocoDividaDto> blocos) {

    public static DividaDto de(Divida divida, BigDecimal totalPendente) {
        Equipa equipa = divida.getEquipa();
        return new DividaDto(
                divida.getId(),
                equipa.getId(),
                equipa.getNome(),
                equipa.getLiga().getId(),
                equipa.getLiga().getNome(),
                divida.getEstado().name(),
                totalPendente,
                divida.getBlocos().stream().map(BlocoDividaDto::de).toList()
        );
    }

    /** Uma equipa sem nenhum bloco registado ainda: nada a dever, sem histórico. */
    public static DividaDto vazia(Equipa equipa) {
        return new DividaDto(
                null,
                equipa.getId(),
                equipa.getNome(),
                equipa.getLiga().getId(),
                equipa.getLiga().getNome(),
                "SEM_DIVIDA",
                BigDecimal.ZERO,
                List.of()
        );
    }
}
