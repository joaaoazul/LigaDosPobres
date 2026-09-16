package com.ligarecord.web;

/**
 * Lançada quando uma origem esgotou os pedidos que pode fazer a um endereço
 * aberto ao público numa janela de tempo — hoje, o formulário de contacto
 * (ver {@code SuporteService}).
 *
 * <p>Separada da {@link LoginBloqueadoException}, que diz a mesma coisa ao
 * cliente (429) mas sobre outra coisa: aquela é sobre um email a falhar
 * passwords, esta é sobre uma origem a despejar mensagens. Juntá-las numa só
 * poupava uma classe e passava a dar a mesma mensagem a dois problemas que
 * quem lê os logs precisa de distinguir.
 */
public class PedidosDemaisException extends RuntimeException {

    public PedidosDemaisException(String mensagem) {
        super(mensagem);
    }
}
