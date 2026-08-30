package com.ligarecord.web.dto;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoEquipa;

import java.util.UUID;

public record LigaDto(
        UUID id,
        String nome,
        String estado,
        int maxEquipas,
        int totalEquipas,
        int equipasAtivas,
        int totalJornadas) {

    public static LigaDto de(Liga liga) {
        int ativas = 0;
        for (Equipa equipa : liga.getEquipas()) {
            if (equipa.getEstado() == EstadoEquipa.ATIVA) {
                ativas++;
            }
        }
        return new LigaDto(
                liga.getId(),
                liga.getNome(),
                liga.getEstado().name(),
                liga.getMaxEquipas(),
                liga.getEquipas().size(),
                ativas,
                liga.getJornadas().size()
        );
    }
}
