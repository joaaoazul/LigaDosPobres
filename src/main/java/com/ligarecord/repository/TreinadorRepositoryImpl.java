package com.ligarecord.repository;

import com.ligarecord.domain.Treinador;

public class TreinadorRepositoryImpl extends RepositorioEmMemoria<Treinador> implements TreinadorRepository {

    @Override
    public Treinador guardar(Treinador treinador) {
        return super.guardar(treinador);
    }
}
