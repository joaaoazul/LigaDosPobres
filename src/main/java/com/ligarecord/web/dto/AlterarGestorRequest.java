package com.ligarecord.web.dto;

/** Cada pedido altera um campo: os outros vêm a null exceto o que se quer mudar. */
public record AlterarGestorRequest(Boolean ativo, String papel, Boolean podeCriarLigas, String email) {
}
