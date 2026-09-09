package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.enums.EstadoEquipa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ClassificacaoService {

    /**
     * Soma os pontos das jornadas da liga e ordena as equipas por pontos
     * decrescentes. As equipas desistentes ficam sempre nas últimas posições.
     *
     * <p>Que jornadas contam depende da liga: por omissão contam todas, mas há
     * ligas em que o treino é um aquecimento e a tabela só começa a contar
     * quando as oficiais arrancam (ver {@link Liga#isPontosTreinoContam()}).
     */
    public List<ClassificacaoGeral> calcularClassificacao (Liga liga){
        if (liga == null){
            throw new IllegalArgumentException("A liga é obrigatória.");
        }

        Map<UUID, Integer> pontosPorEquipa = new HashMap<>();
        for (Equipa equipa : liga.getEquipas()){
            pontosPorEquipa.put(equipa.getId(), 0);
        }

        for (Jornada jornada : liga.getJornadas()){
            if (jornada.iseTreino() && !liga.isPontosTreinoContam()){
                continue;
            }
            for (ResultadoJornada resultado : jornada.getResultadoJ()){
                UUID equipaId = resultado.getEquipa().getId();
                pontosPorEquipa.merge(equipaId, resultado.getPontuacao(), Integer::sum);
            }
        }

        List<Equipa> ordenadas = new ArrayList<>(liga.getEquipas());
        ordenadas.sort(
                Comparator.comparing((Equipa equipa) -> equipa.getEstado() == EstadoEquipa.DESISTENTE)
                        .thenComparing(equipa -> pontosPorEquipa.getOrDefault(equipa.getId(), 0),
                                Comparator.reverseOrder())
                        // Só decide entre equipas com os mesmos pontos — ver
                        // Equipa.ordemDesempate. Sem desempate resolvido para
                        // este empate (ou para nenhum), fica tudo null e o
                        // nome continua a decidir, como sempre decidiu.
                        .thenComparing(equipa -> ordemDesempateValida(equipa, pontosPorEquipa),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Equipa::getNome, String.CASE_INSENSITIVE_ORDER)
        );

        List<ClassificacaoGeral> classificacao = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i++){
            Equipa equipa = ordenadas.get(i);
            classificacao.add(new ClassificacaoGeral(
                    UUID.randomUUID(),
                    equipa,
                    i + 1,
                    pontosPorEquipa.getOrDefault(equipa.getId(), 0)
            ));
        }

        return classificacao;
    }

    /**
     * O {@code ordemDesempate} da equipa, mas só se ainda for sobre o mesmo
     * empate em que foi fixado — {@code ordemDesempatePontos} guarda os
     * pontos desse empate. Sem isto, um desempate resolvido a 10 pontos
     * ficava a decidir também um empate posterior e completamente diferente,
     * a 15 pontos, entre outras equipas que nunca chegaram a ser comparadas.
     */
    private static Integer ordemDesempateValida(Equipa equipa, Map<UUID, Integer> pontosPorEquipa) {
        Integer pontosDoDesempate = equipa.getOrdemDesempatePontos();
        if (pontosDoDesempate == null) {
            return null;
        }
        Integer pontosAtuais = pontosPorEquipa.getOrDefault(equipa.getId(), 0);
        return pontosDoDesempate.equals(pontosAtuais) ? equipa.getOrdemDesempate() : null;
    }

    /**
     * Os grupos de equipas ativas empatadas em pontos na classificação geral
     * — hoje desempatadas por nome. Espelha
     * {@code JornadaService.resultadosEmpatados} e
     * {@code CobrancaPeriodoService.empatesPorDesfazer}: cada equipa entra,
     * no máximo, num grupo, porque se agrupa por pontuação. Desistentes ficam
     * de fora — já vão sempre para o fim, pontos à parte.
     */
    public List<List<Equipa>> gruposEmpatados(Liga liga) {
        Map<Integer, List<Equipa>> pontosPorEquipa = new LinkedHashMap<>();
        for (ClassificacaoGeral linha : calcularClassificacao(liga)) {
            if (linha.getEquipa().getEstado() != EstadoEquipa.ATIVA) {
                continue;
            }
            pontosPorEquipa.computeIfAbsent(linha.getPontosAcumulados(), pontos -> new ArrayList<>())
                    .add(linha.getEquipa());
        }
        return pontosPorEquipa.values().stream().filter(grupo -> grupo.size() > 1).toList();
    }

    /**
     * Fixa a ordem manual de desempate entre as equipas empatadas indicadas,
     * e devolve as equipas cujo {@code ordemDesempate} mudou — para quem
     * chama as gravar.
     *
     * <p>{@code ordem} tem de conter, uma só vez, exactamente as equipas de
     * todos os grupos empatados da liga — a mesma garantia usada no
     * desempate de uma jornada: a ordem só decide dentro do grupo de pontos a
     * que a equipa pertence, nunca troca equipas entre grupos diferentes.
     */
    public List<Equipa> aplicarDesempate(Liga liga, List<UUID> ordem) {
        if (ordem == null || ordem.isEmpty()) {
            throw new IllegalArgumentException("Indica a ordem das equipas empatadas.");
        }

        List<List<Equipa>> grupos = gruposEmpatados(liga);
        if (grupos.isEmpty()) {
            throw new IllegalStateException("Não há equipas empatadas para desempatar.");
        }

        Set<UUID> empatadas = grupos.stream().flatMap(List::stream)
                .map(Equipa::getId).collect(Collectors.toSet());
        Set<UUID> indicadas = new HashSet<>(ordem);
        if (indicadas.size() != ordem.size() || !indicadas.equals(empatadas)) {
            throw new IllegalArgumentException(
                    "A ordem tem de indicar, uma só vez, exactamente as equipas empatadas.");
        }

        Map<UUID, Integer> lugarNaOrdem = new HashMap<>();
        for (int i = 0; i < ordem.size(); i++) {
            lugarNaOrdem.put(ordem.get(i), i);
        }

        Map<UUID, Integer> pontosPorEquipa = new HashMap<>();
        for (ClassificacaoGeral linha : calcularClassificacao(liga)) {
            pontosPorEquipa.put(linha.getEquipa().getId(), linha.getPontosAcumulados());
        }

        List<Equipa> alteradas = new ArrayList<>();
        for (List<Equipa> grupo : grupos) {
            List<Equipa> ordenado = grupo.stream()
                    .sorted(Comparator.comparingInt(equipa -> lugarNaOrdem.get(equipa.getId())))
                    .toList();
            for (int i = 0; i < ordenado.size(); i++) {
                Equipa equipa = ordenado.get(i);
                equipa.setOrdemDesempate(i);
                equipa.setOrdemDesempatePontos(pontosPorEquipa.get(equipa.getId()));
                alteradas.add(equipa);
            }
        }
        return alteradas;
    }

    public List<Equipa> listarEquipas (Liga liga){
        return liga.getEquipas();
    }

    public List<Equipa> listarEquipasAtivas (Liga liga){
        return liga.getEquipas()
                .stream()
                .filter(equipa -> equipa.getEstado() == EstadoEquipa.ATIVA)
                .toList();
    }

    /**
     * O valor do período para quem está nesta posição, segundo a regra da liga
     * — da fórmula ou da tabela, conforme a escala que a regra usa.
     *
     * <p>A conta em si vive na {@link RegraDivida}, que é quem sabe qual das
     * duas está em vigor. Isto fica como porta de entrada porque é por aqui que
     * o resto da aplicação lhe chama.
     */
    public BigDecimal calcularValor(RegraDivida regra, int posicao) {
        return regra.valorDaPosicao(posicao);
    }


}
