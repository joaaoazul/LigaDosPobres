package com.ligarecord.web.dto;

import java.util.List;

/**
 * O resultado de convidar os treinadores em falta de uma liga, equipa a equipa.
 *
 * <p>O {@code link} vem preenchido em tudo o que ficou com convite por usar,
 * tenha o email saído ou não: é com estes que o gestor faz a mensagem que vai
 * entregar à mão. Nas equipas que ficaram de fora vem a null.
 */
public record ConviteEmMassaDto(List<Linha> equipas, int convidadas, int enviadas) {

    /**
     * {@code estado} é um de: {@code ENVIADO}, {@code SEM_EMAIL},
     * {@code LIMITE_ATINGIDO}, {@code FALHOU} — os quatro fins de uma tentativa
     * de entrega — ou {@code JA_TEM_CONTA} e {@code DESISTENTE}, para as que
     * nem chegaram a ser convidadas.
     */
    public record Linha(String equipa, String treinador, String estado, String link) {
    }
}
