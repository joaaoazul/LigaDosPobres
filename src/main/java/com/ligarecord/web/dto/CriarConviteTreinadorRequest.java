package com.ligarecord.web.dto;

/**
 * {@code diasValidade} a null usa a validade por omissão do serviço. Não existe
 * forma de pedir um convite sem prazo: é uma credencial, e uma credencial sem
 * prazo fica a valer numa conversa de WhatsApp para sempre.
 */
public record CriarConviteTreinadorRequest(Integer diasValidade) {
}
