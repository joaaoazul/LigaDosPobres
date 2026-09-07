package com.ligarecord.web.dto;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.CobrancaPeriodo;
import com.ligarecord.domain.CobrancaValor;
import com.ligarecord.service.EscalaColada;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uma cobrança presa a uma jornada, e em que pé está.
 *
 * <p>{@code estado} é {@code POR_COBRAR}, {@code A_ESPERA_DE_DESEMPATE} — e aí
 * {@code empates} traz os grupos que o gestor tem de ordenar — ou
 * {@code COBRADA}.
 */
public record CobrancaPeriodoDto(
        UUID id,
        String nome,
        int jornadaOficial,
        String tabela,
        String estado,
        Instant cobradaEm,
        List<Empate> empates) {

    /** Um grupo de equipas empatadas em pontos que pagariam valores diferentes. */
    public record Empate(int pontos, List<EquipaEmpatada> equipas) {
    }

    public record EquipaEmpatada(UUID equipaId, String equipa, int posicao) {
    }

    public static CobrancaPeriodoDto de(CobrancaPeriodo cobranca, List<List<ClassificacaoGeral>> empates) {
        List<Empate> grupos = empates.stream()
                .map(grupo -> new Empate(
                        grupo.get(0).getPontosAcumulados(),
                        grupo.stream()
                                .map(linha -> new EquipaEmpatada(
                                        linha.getEquipa().getId(),
                                        linha.getEquipa().getNome(),
                                        linha.getPosicao()))
                                .toList()))
                .toList();

        return new CobrancaPeriodoDto(
                cobranca.getId(),
                cobranca.getNome(),
                cobranca.getJornadaOficial(),
                EscalaColada.escrever(cobranca.getTabela().stream().map(CobrancaValor::getValor).toList()),
                cobranca.estaCobrada() ? "COBRADA" : (grupos.isEmpty() ? "POR_COBRAR" : "A_ESPERA_DE_DESEMPATE"),
                cobranca.getCobradaEm(),
                grupos
        );
    }
}
