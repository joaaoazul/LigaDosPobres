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
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
