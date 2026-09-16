package com.ligarecord.web.dto;

/** O que alguém escreveu no formulário de contacto da página de entrada. */
public record MensagemSuporteRequest(
        String nome,
        String email,
        String assunto,
        String mensagem) {
}
