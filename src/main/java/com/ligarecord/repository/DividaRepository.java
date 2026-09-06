package com.ligarecord.repository;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoDivida;

import java.util.List;
import java.util.Optional;

public interface DividaRepository {

    Divida guardarDivida(Divida divida);

    Optional<Divida> buscarPorEquipa(Equipa equipa);

    List<Divida> listarDividas(Liga liga, EstadoDivida estadoDivida);

    /**
     * Todas as dívidas da liga, sem filtrar por estado.
     *
     * <p>Distinto do {@link #listarDividas}: aquele filtra pelo estado da
     * dívida da equipa, que não diz nada sobre cada bloco — uma dívida
     * pendente pode ter blocos já pagos lá dentro. Para somar dinheiro é
     * preciso olhar bloco a bloco, e para isso são precisas todas.
     */
    List<Divida> listarPorLiga(Liga liga);

    /** Todas as dívidas das equipas que esta conta treina, em qualquer liga. */
    List<Divida> buscarPorTreinador(Gestor conta);
}
