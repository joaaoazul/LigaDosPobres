package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.JornadaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassificacaoService {

    private static final int TAMANHO_ESCALAO = 5;
    private static final BigDecimal VALOR_POR_ESCALAO = new BigDecimal("0.5");

    public List<ClassificacaoGeral> calcularClassificacao (Liga liga){
        return null;
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
