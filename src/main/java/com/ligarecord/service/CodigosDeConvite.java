package com.ligarecord.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração dos códigos de convite, para gestores e para treinadores.
 *
 * <p>Um código por usar é uma credencial: quem o tiver cria uma conta. O
 * tamanho está aqui num sítio só para não haver um convite gerado com menos
 * entropia do que o outro por distração.
 */
final class CodigosDeConvite {

    /** 24 bytes: demasiado grande para ser adivinhado por tentativa e erro. */
    private static final int BYTES = 24;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigosDeConvite() {
    }

    static String gerar() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
