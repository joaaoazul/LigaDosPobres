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

    @Override
    public void enviar(String para, String assunto, String texto, String html) {
        // Só a versão de texto: é a legível numa consola, e traz o link à
        // mesma, que é para isto que serve em desenvolvimento.
        LOG.info("Email por enviar, não há chave configurada\npara: {}\nassunto: {}\n{}", para, assunto, texto);
    }
}
