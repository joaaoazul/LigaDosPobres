package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.enums.EstadoEquipa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassificacaoService {

    private static final int TAMANHO_ESCALAO = 5;
    private static final BigDecimal VALOR_POR_ESCALAO = new BigDecimal("0.5");

    /**
     * Soma os pontos de todas as jornadas da liga e ordena as equipas por pontos
     * decrescentes. As equipas desistentes ficam sempre nas últimas posições.
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

    public List<Equipa> listarEquipas (Liga liga){
        return liga.getEquipas();
    }

    public List<Equipa> listarEquipasAtivas (Liga liga){
        return liga.getEquipas()
                .stream()
                .filter(equipa -> equipa.getEstado() == EstadoEquipa.ATIVA)
                .toList();
    }

    public BigDecimal calcularValor (int numEquipasAtivas, int posicao){
        int escalao = (posicao - 1) / TAMANHO_ESCALAO;
        return VALOR_POR_ESCALAO.multiply(BigDecimal.valueOf(escalao));
    }


}
