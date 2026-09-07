package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.CobrancaPeriodo;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoEquipa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cobra o "Inverno" e o "Verão": uma vez, na jornada combinada, pela posição de
 * cada equipa na classificação geral.
 *
 * <p><b>Um empate que mude o valor trava a cobrança.</b> É a mesma regra que o
 * {@link JornadaService} já aplica ao fechar uma jornada: cobrar com o empate
 * por desfazer era cobrar a mais a umas e a menos a outras, e com o dinheiro já
 * lançado não há forma limpa de corrigir. A diferença é que aqui o desempate é
 * da classificação geral — que, fora disto, não decide dinheiro nenhum e por
 * isso continua a ser ordenada por nome.
 */
@Service
public class CobrancaPeriodoService {

    private final ClassificacaoService classificacaoService;
    private final DividaService dividaService;

    public CobrancaPeriodoService(ClassificacaoService classificacaoService, DividaService dividaService) {
        this.classificacaoService = classificacaoService;
        this.dividaService = dividaService;
    }

    /**
     * Cobra, se não houver empate por desfazer. Se houver, não faz nada: a
     * cobrança fica por fazer até o gestor ordenar as equipas empatadas.
     */
    @Transactional
    public boolean cobrarSePuder(CobrancaPeriodo cobranca, Liga liga) {
        if (cobranca.estaCobrada()) {
            return false;
        }
        List<ClassificacaoGeral> classificacao = classificacaoService.calcularClassificacao(liga);
        if (!empatesPorDesfazer(cobranca, classificacao).isEmpty()) {
            return false;
        }

        cobrar(cobranca, posicoesDe(classificacao), classificacao);
        return true;
    }

    /**
     * Ordena as equipas empatadas como o gestor disse e cobra a seguir.
     *
     * <p>A ordem é uma lista só, com todas as equipas empatadas. O servidor
     * volta a arrumá-las dentro do seu grupo de pontos, por isso uma lista mal
     * ordenada nunca consegue trocar equipas entre pontuações diferentes — é a
     * mesma garantia do desempate de uma jornada.
     */
    @Transactional
    public void resolverDesempateECobrar(CobrancaPeriodo cobranca, Liga liga, List<UUID> ordem) {
        if (cobranca.estaCobrada()) {
            throw new IllegalStateException("A cobrança \"" + cobranca.getNome() + "\" já foi feita.");
        }
        if (ordem == null || ordem.isEmpty()) {
            throw new IllegalArgumentException("Indica a ordem das equipas empatadas.");
        }

        List<ClassificacaoGeral> classificacao = classificacaoService.calcularClassificacao(liga);
        List<List<ClassificacaoGeral>> empates = empatesPorDesfazer(cobranca, classificacao);
        if (empates.isEmpty()) {
            throw new IllegalStateException("Esta cobrança não está à espera de desempate.");
        }

        Set<UUID> empatadas = new HashSet<>();
        empates.forEach(grupo -> grupo.forEach(linha -> empatadas.add(linha.getEquipa().getId())));
        Set<UUID> indicadas = new HashSet<>(ordem);
        if (indicadas.size() != ordem.size() || !indicadas.equals(empatadas)) {
            throw new IllegalArgumentException(
                    "A ordem tem de indicar, uma só vez, exactamente as equipas empatadas.");
        }

        Map<UUID, Integer> posicoes = posicoesDe(classificacao);
        for (List<ClassificacaoGeral> grupo : empates) {
            // Os lugares que este grupo ocupa, por ordem, redistribuídos pela
            // ordem que o gestor deu.
            List<Integer> lugares = grupo.stream().map(ClassificacaoGeral::getPosicao).sorted().toList();
            List<UUID> desteGrupo = ordem.stream()
                    .filter(id -> grupo.stream().anyMatch(linha -> linha.getEquipa().getId().equals(id)))
                    .toList();
            for (int i = 0; i < desteGrupo.size(); i++) {
                posicoes.put(desteGrupo.get(i), lugares.get(i));
            }
        }

        cobrar(cobranca, posicoes, classificacao);
    }

    /**
     * Os grupos de equipas empatadas em pontos que pagariam valores diferentes.
     *
     * <p>Um empate entre duas posições que pagam o mesmo não é problema
     * nenhum, e travar a cobrança por causa dele era pedir ao gestor um
     * desempate que não muda nada. Equipas desistentes ficam de fora: não são
     * cobradas, e o lugar em que aparecem não lhes custa dinheiro.
     */
    public List<List<ClassificacaoGeral>> empatesPorDesfazer(CobrancaPeriodo cobranca,
                                                            List<ClassificacaoGeral> classificacao) {
        Map<Integer, List<ClassificacaoGeral>> porPontos = new LinkedHashMap<>();
        for (ClassificacaoGeral linha : classificacao) {
            if (linha.getEquipa().getEstado() != EstadoEquipa.ATIVA) {
                continue;
            }
            porPontos.computeIfAbsent(linha.getPontosAcumulados(), pontos -> new ArrayList<>()).add(linha);
        }

        List<List<ClassificacaoGeral>> empates = new ArrayList<>();
        for (List<ClassificacaoGeral> grupo : porPontos.values()) {
            if (grupo.size() < 2) {
                continue;
            }
            BigDecimal primeiro = cobranca.valorDaPosicao(grupo.get(0).getPosicao());
            BigDecimal ultimo = cobranca.valorDaPosicao(grupo.get(grupo.size() - 1).getPosicao());
            if (primeiro.compareTo(ultimo) != 0) {
                empates.add(grupo);
            }
        }
        return empates;
    }

    private void cobrar(CobrancaPeriodo cobranca, Map<UUID, Integer> posicoes,
                        List<ClassificacaoGeral> classificacao) {
        for (ClassificacaoGeral linha : classificacao) {
            Equipa equipa = linha.getEquipa();
            if (equipa.getEstado() != EstadoEquipa.ATIVA) {
                continue;
            }
            dividaService.registarCobranca(equipa, cobranca.getNome(),
                    cobranca.valorDaPosicao(posicoes.get(equipa.getId())));
        }
        cobranca.marcarCobrada();
    }

    private Map<UUID, Integer> posicoesDe(List<ClassificacaoGeral> classificacao) {
        Map<UUID, Integer> posicoes = new HashMap<>();
        classificacao.forEach(linha -> posicoes.put(linha.getEquipa().getId(), linha.getPosicao()));
        return posicoes;
    }
}
