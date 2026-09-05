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

    /**
     * O valor do período para quem está nesta posição, segundo a regra da
     * liga: sobe {@code incremento} a cada {@code equipasPorEscalao}
     * posições, a partir de {@code valorInicial}, sem nunca passar de
     * {@code valorMaximo}.
     */
    public BigDecimal calcularValor(RegraDivida regra, int posicao) {
        int escalao = (posicao - 1) / regra.getEquipasPorEscalao();
        BigDecimal valor = regra.getValorInicial().add(regra.getIncremento().multiply(BigDecimal.valueOf(escalao)));
        return valor.min(regra.getValorMaximo());
    }


}
