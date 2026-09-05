package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;

import java.util.List;

public interface TreinadorRepository {

    Treinador guardar(Treinador treinador);

    /** Todos os Treinador ligados a esta conta, em qualquer liga. */
    List<Treinador> buscarPorConta(Gestor conta);
}
