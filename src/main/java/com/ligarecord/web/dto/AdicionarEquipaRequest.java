package com.ligarecord.web.dto;

/** {@code treinadorEmail} é opcional: sem ele o convite entrega-se pelo link. */
public record AdicionarEquipaRequest(String nome, String treinador, String treinadorEmail) {
}
