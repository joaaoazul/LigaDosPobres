package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Liga;

import java.time.Instant;

/**
 * O que a página do convite mostra a quem chega pelo link, sem sessão nenhuma:
 * o suficiente para a pessoa reconhecer que o convite é para ela — quem
 * convidou, para que equipa, em que liga — e nada mais.
 *
 * <p><b>Não leva o código.</b> Quem abre a página já o traz no endereço; devolvê-lo
 * outra vez só o punha a passear. E não leva contacto nenhum: nem o email do
 * treinador, nem o do gestor.
 */
public record ConvitePublicoDto(
        String treinadorNome,
        String equipaNome,
        String ligaNome,
        String gestorNome,
        Instant expiraEm) {

    public static ConvitePublicoDto de(ConviteTreinador convite) {
        Liga liga = convite.getEquipa().getLiga();
        return new ConvitePublicoDto(
                convite.getTreinador().getNome(),
                convite.getEquipa().getNome(),
                liga == null ? null : liga.getNome(),
                convite.getCriadoPor().getNome(),
                convite.getExpiraEm()
        );
    }
}
