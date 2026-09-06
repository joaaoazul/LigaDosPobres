package com.ligarecord.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração dos códigos que valem por uma credencial: convites de gestor, de
 * treinador, e o link de recuperação de password.
 *
 * <p>Cada um deles dá acesso a alguma coisa a quem o tiver. O tamanho está aqui
 * num sítio só para não haver um gerado com menos entropia do que o outro por
 * distração.
 */
final class CodigosAleatorios {

    /** 24 bytes: demasiado grande para ser adivinhado por tentativa e erro. */
    private static final int BYTES = 24;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigosAleatorios() {
    }

    static String gerar() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
