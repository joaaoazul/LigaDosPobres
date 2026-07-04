package com.ligarecord.repository;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Treinador;

import java.util.List;

public interface EquipaRepository {
    Equipa guardar(Equipa equipa);
    List<Equipa> buscarPorTreinador(Treinador treinador);

}
