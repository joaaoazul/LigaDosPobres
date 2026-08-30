package com.ligarecord.web.dto;

import com.ligarecord.security.GestorAutenticado;

import java.util.UUID;

public record GestorDto(UUID id, String nome, String email, boolean admin) {

    public static GestorDto de(GestorAutenticado gestor) {
        return new GestorDto(gestor.getId(), gestor.getNome(), gestor.getEmail(), gestor.isAdmin());
    }
}
