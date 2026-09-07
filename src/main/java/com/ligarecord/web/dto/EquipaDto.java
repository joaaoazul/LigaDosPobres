package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma equipa, e — só para quem gere a liga — o estado da conta do treinador.
 *
 * <p>O nome do treinador é sempre o rótulo que o gestor escreveu, mesmo depois
 * de haver conta ligada: a lista de equipas é dele, e o nome da conta é outra
 * coisa, o da pessoa.
 *
 * <p>Os cinco campos do fim — o email do lugar incluído — vêm a nulo na vista do
 * treinador ({@code de}) e só são preenchidos para o gestor
 * ({@code deParaGestor}). O convite propriamente
 * dito — código e link — nunca vem aqui: é uma credencial, e sairia em todos os
 * pedidos de detalhe da liga em vez de sair quando alguém carrega no botão.
 */
public record EquipaDto(
        UUID id,
        String nome,
        String treinador,
        String estado,
        String treinadorEmail,
        Boolean treinadorTemConta,
        String conviteEstado,
        UUID conviteId,
        Instant conviteExpiraEm) {

    public static EquipaDto de(Equipa equipa) {
        return new EquipaDto(
                equipa.getId(),
                equipa.getNome(),
                equipa.getTreinador() == null ? null : equipa.getTreinador().getNome(),
                equipa.getEstado().name(),
                null, null, null, null, null
        );
    }

    /** {@code pendente} a null quando não há convite por usar para esta equipa. */
    public static EquipaDto deParaGestor(Equipa equipa, ConviteTreinador pendente) {
        boolean temConta = equipa.getTreinador() != null && equipa.getTreinador().temConta();
        return new EquipaDto(
                equipa.getId(),
                equipa.getNome(),
                equipa.getTreinador() == null ? null : equipa.getTreinador().getNome(),
                equipa.getEstado().name(),
                equipa.getTreinador() == null ? null : equipa.getTreinador().getEmail(),
                temConta,
                temConta ? "LIGADA" : (pendente == null ? "SEM_CONVITE" : "PENDENTE"),
                pendente == null ? null : pendente.getId(),
                pendente == null ? null : pendente.getExpiraEm()
        );
    }
}
