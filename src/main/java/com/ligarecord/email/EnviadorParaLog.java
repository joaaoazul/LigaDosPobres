package com.ligarecord.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escreve a mensagem no log em vez de a enviar. É o que corre quando não há
 * chave do Resend configurada: em desenvolvimento, o link de recuperação sai na
 * consola e o fluxo inteiro pode ser experimentado sem servidor de email
 * nenhum.
 *
 * <p>Se isto aparecer nos logs de produção é porque falta o {@code EMAIL_CHAVE},
 * e o aviso no arranque existe para isso não passar despercebido.
 */
public class EnviadorParaLog implements EnviadorDeEmail {

    private static final Logger LOG = LoggerFactory.getLogger(EnviadorParaLog.class);

    /**
     * Devolve {@code false}, porque é a verdade: nada saiu. Quem pergunta —
     * hoje só o convite de treinador — passa a dizer ao gestor que o email não
     * foi enviado e que entregue o link à mão, em vez de lhe prometer um envio
     * que não houve. Em produção isto só acontece com a chave por configurar, e
     * é assim que se dá por isso sem ler os logs.
     */
    @Override
    public boolean enviar(String para, String assunto, String texto, String html) {
        // Só a versão de texto: é a legível numa consola, e traz o link à
        // mesma, que é para isto que serve em desenvolvimento.
        LOG.info("Email por enviar, não há chave configurada\npara: {}\nassunto: {}\n{}", para, assunto, texto);
        return false;
    }
}
