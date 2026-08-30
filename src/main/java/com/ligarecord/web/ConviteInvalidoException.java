package com.ligarecord.web;

/**
 * Lançada quando o registo é tentado sem um código de convite válido.
 */
public class ConviteInvalidoException extends RuntimeException {

    public ConviteInvalidoException(String mensagem) {
        super(mensagem);
    }
}
