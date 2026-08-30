package com.ligarecord.web.dto;

/** {@code diasValidade} a null significa um convite sem prazo. */
public record CriarConviteRequest(String nota, Integer diasValidade) {
}
