package com.ligarecord.web.dto;

import com.ligarecord.domain.Gestor;
import com.ligarecord.security.GestorAutenticado;

import java.time.Instant;
import java.util.UUID;

public record GestorDto(
        UUID id,
        String nome,
        String email,
        boolean admin,
        boolean podeCriarLigas,
        boolean licencaAtiva,
        boolean emTrial,
        long diasLicencaRestantes,
        Instant licencaExpiraEm) {

    public static GestorDto de(GestorAutenticado gestor) {
        return new GestorDto(gestor.getId(), gestor.getNome(), gestor.getEmail(),
                gestor.isAdmin(), gestor.isPodeCriarLigas(),
                gestor.isLicencaAtiva(), gestor.isEmTrial(),
                gestor.getDiasLicencaRestantes(), gestor.getLicencaExpiraEm());
    }

    /** Para logo a seguir a um registo, antes de existir uma sessão com {@link GestorAutenticado}. */
    public static GestorDto de(Gestor gestor) {
        return new GestorDto(gestor.getId(), gestor.getNome(), gestor.getEmail(),
                gestor.isAdmin(), gestor.isPodeCriarLigas(),
                gestor.licencaAtiva(), gestor.emTrial(),
                gestor.diasLicencaRestantes(), gestor.licencaExpiraEm());
    }
}
