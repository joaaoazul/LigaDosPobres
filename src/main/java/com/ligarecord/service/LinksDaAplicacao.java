package com.ligarecord.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Os endereços que a aplicação manda para fora — no email da recuperação de
 * password, e no convite que o gestor entrega ao treinador.
 *
 * <p>Existe para o {@code app.url} ser lido e limpo num sítio só. Enquanto cada
 * serviço o lia por si, cada um tinha a sua cópia do detalhe de tirar a barra
 * final, e bastava um esquecer-se para sair um link com {@code //} pelo meio.
 */
@Component
public class LinksDaAplicacao {

    private final String base;

    public LinksDaAplicacao(@Value("${app.url:http://localhost:8080}") String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * A página onde o treinador aceita o convite.
     *
     * <p>O código não é escapado porque não precisa: sai do
     * {@code CodigosAleatorios} em base64url, que só tem letras, dígitos,
     * {@code -} e {@code _}.
     */
    public String convite(String codigo) {
        return base + "/convite.html?c=" + codigo;
    }

    public String novaPassword(String codigo) {
        return base + "/nova-password.html?codigo=" + codigo;
    }
}
