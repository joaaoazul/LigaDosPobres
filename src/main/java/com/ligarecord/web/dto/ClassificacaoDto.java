package com.ligarecord.web.dto;

import com.ligarecord.domain.ClassificacaoGeral;

import java.util.UUID;

public record ClassificacaoDto(
        int posicao,
        UUID equipaId,
        String equipa,
        String treinador,
        String estado,
        int pontos) {

    public static ClassificacaoDto de(ClassificacaoGeral linha) {
        return new ClassificacaoDto(
                linha.getPosicao(),
                linha.getEquipa().getId(),
                linha.getEquipa().getNome(),
                linha.getEquipa().getTreinador() == null ? null : linha.getEquipa().getTreinador().getNome(),
                linha.getEquipa().getEstado().name(),
                linha.getPontosAcumulados()
        );
    }
}
