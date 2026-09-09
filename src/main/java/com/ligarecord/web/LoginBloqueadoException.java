package com.ligarecord.web;

/**
 * Lançada quando um email esgotou as tentativas de login na janela de tempo
 * (ver {@code LimiteDeLoginService}).
 */
public class LoginBloqueadoException extends RuntimeException {

    public LoginBloqueadoException(String mensagem) {
        super(mensagem);
    }
}
