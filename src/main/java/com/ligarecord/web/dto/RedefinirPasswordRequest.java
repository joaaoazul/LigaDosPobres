package com.ligarecord.web.dto;

/** O código vem do link do email; a password é a nova, escrita na página. */
public record RedefinirPasswordRequest(String codigo, String password) {
}
