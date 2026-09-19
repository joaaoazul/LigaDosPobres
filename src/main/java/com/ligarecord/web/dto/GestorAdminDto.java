package com.ligarecord.web.dto;

import com.ligarecord.domain.Gestor;

import java.time.Instant;
import java.util.UUID;

public record GestorAdminDto(
        UUID id,
        String nome,
        String email,
        String papel,
        boolean ativo,
        boolean podeCriarLigas,
        Instant criadoEm,
        boolean licencaAtiva,
        boolean emTrial,
        long diasLicencaRestantes,
        Instant licencaExpiraEm) {

    public static GestorAdminDto de(Gestor gestor) {
        return new GestorAdminDto(
                gestor.getId(),
                gestor.getNome(),
                gestor.getEmail(),
                gestor.getPapel().name(),
                gestor.isAtivo(),
                gestor.isPodeCriarLigas(),
                gestor.getCriadoEm(),
                gestor.licencaAtiva(),
                gestor.emTrial(),
                gestor.diasLicencaRestantes(),
                gestor.licencaExpiraEm()
        );
    }
}
